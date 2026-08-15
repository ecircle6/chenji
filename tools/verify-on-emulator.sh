#!/usr/bin/env bash
# =============================================================================
# 一键上模拟器验证（功能代码变更后的标准验证入口）
#
# 流程：构建 Debug APK → 确保模拟器在跑（不在则自动启动指定 AVD）→ 安装
#       （自动处理签名不匹配：先卸载再装，数据会清空）→ 启动 App → 截图留档
#
# 用法（仓库根目录）：
#   bash tools/verify-on-emulator.sh [AVD名]     # 默认 AVD：chenji_test
#
# 依赖：JDK 17+、Android SDK（local.properties 的 sdk.dir 或 Windows 默认路径）
# 注意：模拟器窗口保持打开，人工确认功能效果；改动后再跑一次本脚本即可。
# =============================================================================

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$REPO_ROOT"

echo_error() { echo "❌ $*" >&2; exit 1; }

# ---------- 1. 定位 Android SDK 与 adb ----------
SDK_DIR=""
if [ -f local.properties ]; then
    SDK_DIR="$(grep -E '^sdk\.dir=' local.properties | head -1 | cut -d= -f2)"
fi
[ -z "$SDK_DIR" ] && SDK_DIR="$HOME/AppData/Local/Android/Sdk"
# 去掉可能的引号/尾斜杠
SDK_DIR="${SDK_DIR//\"/}"
SDK_DIR="${SDK_DIR%/}"

ADB="$SDK_DIR/platform-tools/adb.exe"
EMULATOR="$SDK_DIR/emulator/emulator.exe"
[ -x "$ADB" ] || echo_error "找不到 adb：$ADB（请确认 local.properties 的 sdk.dir 或 SDK 默认路径）"

AVD="${1:-chenji_test}"

# ---------- 2. 构建 Debug APK ----------
echo "==> [1/5] 构建 Debug APK"
./gradlew assembleDebug >/dev/null || echo_error "构建失败，请看上面的 Gradle 输出"

APK="$(ls -t app/build/outputs/apk/debug/辰记_v*.apk 2>/dev/null | head -1)"
[ -n "$APK" ] || echo_error "找不到 APK 产物（app/build/outputs/apk/debug/辰记_v*.apk）"
echo "    APK：$APK"

# ---------- 3. 确保模拟器运行 ----------
if "$ADB" devices | grep -qE '^emulator-[0-9]+\s+device$'; then
    echo "==> [2/5] 模拟器已在运行"
else
    echo "==> [2/5] 启动模拟器 $AVD（首次启动较慢，请稍候）"
    [ -x "$EMULATOR" ] || echo_error "找不到 emulator：$EMULATOR"
    "$EMULATOR" -avd "$AVD" >/dev/null 2>&1 &
    boot=""
    for _ in $(seq 1 180); do
        boot="$("$ADB" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
        [ "$boot" = "1" ] && break
        sleep 2
    done
    [ "$boot" = "1" ] || echo_error "模拟器 $AVD 启动超时（90 秒）"
    # 解锁屏幕，避免停在锁屏上看不到应用
    "$ADB" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1
    echo "    模拟器已就绪"
fi

# ---------- 4. 安装 APK（签名不匹配先卸载） ----------
echo "==> [3/5] 安装 APK"
# MSYS 下 adb 的参数不能用 POSIX 路径，转成 Windows 路径并关掉路径转换
APK_WIN="$(cygpath -w "$APK" 2>/dev/null || echo "$APK")"
INSTALL_OUT="$(MSYS_NO_PATHCONV=1 "$ADB" install -r "$APK_WIN" 2>&1)" || true
if echo "$INSTALL_OUT" | grep -q '^Success'; then
    echo "    安装成功（覆盖安装）"
elif echo "$INSTALL_OUT" | grep -q 'INSTALL_FAILED_UPDATE_INCOMPATIBLE'; then
    echo "    ⚠ 签名不匹配（模拟器上装的是别的密钥签的包），先卸载再装——应用数据会清空"
    "$ADB" uninstall com.birthapp >/dev/null 2>&1 || true
    INSTALL_OUT="$(MSYS_NO_PATHCONV=1 "$ADB" install "$APK_WIN" 2>&1)" || true
    echo "$INSTALL_OUT" | grep -q '^Success' || echo_error "安装失败：$INSTALL_OUT"
    echo "    已卸载旧包并安装成功"
else
    echo_error "安装失败：$INSTALL_OUT"
fi

# ---------- 5. 启动并截图留档 ----------
echo "==> [4/5] 启动辰记"
"$ADB" shell am start -n com.birthapp/.MainActivity >/dev/null 2>&1
# 首启可能有升级弹窗，按返回关掉；再等界面稳定
sleep 2
"$ADB" shell input keyevent KEYCODE_BACK >/dev/null 2>&1
sleep 3

OUT_DIR="app/build/verify"
mkdir -p "$OUT_DIR"
SHOT="$OUT_DIR/verify_$(date +%Y%m%d_%H%M%S).png"
"$ADB" exec-out screencap -p > "$SHOT" 2>/dev/null
if [ -s "$SHOT" ]; then
    echo "==> [5/5] 截图已保存：$SHOT"
else
    echo "==> [5/5] 截图失败（不影响验证，可手动看模拟器）"
    rm -f "$SHOT"
fi

echo ""
echo "✅ 验证完成：模拟器上已运行最新版，请人工确认功能效果。"
echo "   若确认有问题，改完代码后重新运行：bash tools/verify-on-emulator.sh"
