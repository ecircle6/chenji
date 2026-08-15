#!/usr/bin/env bash
# =============================================================================
# 模拟器验证提醒脚本（本地 pre-push 钩子用，只提醒不阻断）
#
# 作用：检测「功能代码变更」提交时，提醒先上模拟器实际验证效果。
#       与 check-docs-sync.sh 同一设计原则：只提醒，不阻断（exit 0）。
#
# 用法（三种模式，与 check-docs-sync.sh 一致）：
#   bash tools/check-emulator-verify.sh <base> <head>   # 显式指定 commit 范围
#   bash tools/check-emulator-verify.sh                  # 从 stdin 读 pre-push 数据
# =============================================================================

set -u

# 功能代码范围：命中任一模式即视为「功能变更」（与 check-docs-sync.sh 保持一致）
CODE_PATTERNS='^(app/src/main/|app/build\.gradle\.kts$|build\.gradle\.kts$|settings\.gradle\.kts$|gradle\.properties$|gradle/libs\.versions\.toml$|app/proguard-rules\.pro$)'

EMPTY_TREE='4b825dc642cb6eb9a060e54bf8d69288fbee4904'

collect_changed_files() {
    local base="$1" head="$2"
    local base_oid head_oid
    if [ "$base" = "0000000000000000000000000000000000000000" ] || [ -z "$base" ]; then
        base_oid="$EMPTY_TREE"
    else
        base_oid="$base"
    fi
    head_oid="$head"
    git diff --name-only "$base_oid" "$head_oid" 2>/dev/null
}

collect_from_stdin() {
    if [ -p /dev/stdin ] || [ ! -t 0 ]; then
        local local_ref local_oid remote_ref remote_oid
        while read -r local_ref local_oid remote_ref remote_oid; do
            [ -z "$local_oid" ] && continue
            if [ "$remote_oid" = "0000000000000000000000000000000000000000" ]; then
                git diff --name-only "$EMPTY_TREE" "$local_oid" 2>/dev/null
            else
                git diff --name-only "$remote_oid" "$local_oid" 2>/dev/null
            fi
        done
    else
        if git rev-parse --verify --quiet @{upstream} >/dev/null 2>&1; then
            git diff --name-only "@{upstream}" HEAD 2>/dev/null
        else
            git diff --name-only "$EMPTY_TREE" HEAD 2>/dev/null
        fi
    fi
}

main() {
    local base="${1:-}" head="${2:-}" files
    if [ -n "$base" ] && [ -n "$head" ]; then
        files="$(collect_changed_files "$base" "$head")"
    else
        files="$(collect_from_stdin)"
    fi
    [ -z "$files" ] && exit 0

    if echo "$files" | grep -E "$CODE_PATTERNS" >/dev/null 2>&1; then
        echo ""
        echo "┌──────────────────────────────────────────────────────────────┐"
        echo "│ 📱 模拟器验证提醒：本次提交包含功能代码，建议先上模拟器        │"
        echo "│    实际验证效果后再推送：                                      │"
        echo "│       bash tools/verify-on-emulator.sh                        │"
        echo "│    （一键：构建 → 启动模拟器 → 安装 → 启动 → 截图留档）        │"
        echo "│    （提示仅供提醒，不阻断本次推送）                            │"
        echo "└──────────────────────────────────────────────────────────────┘"
    fi

    exit 0
}

main "$@"
