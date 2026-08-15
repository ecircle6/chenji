#!/usr/bin/env bash
# =============================================================================
# 一键安装文档同步检测钩子（pre-push）
#
# 用法（在仓库根目录，Git Bash / Linux / macOS 均可）：
#   bash tools/install-hooks.sh
#
# 每台开发电脑克隆仓库后运行一次即可；升级钩子逻辑时重新运行覆盖。
# =============================================================================

set -e

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
HOOK_SRC="$REPO_ROOT/tools/hooks/pre-push"
HOOK_DST="$REPO_ROOT/.git/hooks/pre-push"

if [ ! -d "$REPO_ROOT/.git" ]; then
    echo "错误：$REPO_ROOT 不是 git 仓库根目录" >&2
    exit 1
fi

if [ ! -f "$HOOK_SRC" ]; then
    echo "错误：钩子源文件不存在：$HOOK_SRC" >&2
    exit 1
fi

cp "$HOOK_SRC" "$HOOK_DST"
chmod +x "$HOOK_DST"

echo "✅ pre-push 钩子已安装：$HOOK_DST"
echo "   推送时会提醒两件事（均不阻断）："
echo "   ① 功能代码变更而 README.md / TODO.md 未同步"
echo "   ② 功能代码变更时记得先上模拟器验证（bash tools/verify-on-emulator.sh）"
