#!/usr/bin/env bash
# =============================================================================
# 文档同步检测脚本（本地 git 钩子与 GitHub Actions 共用）
#
# 作用：检测「功能代码变更」提交时，README.md / TODO.md 是否同步更新。
#       功能代码变了而两个文档都没动，说明文档大概率过时了 —— 输出提醒。
#
# 设计原则：只提醒，不阻断（exit 0）。是否同步文档由开发者决定。
#
# 用法（三种模式，按顺序尝试）：
#   bash tools/check-docs-sync.sh <base> <head>   # ① 显式指定 commit 范围（CI 用）
#   bash tools/check-docs-sync.sh <base> <head> <"stdin">
#        # ② 第三参为任意非空字符串时，忽略 stdin 直接比较两个 commit（本地钩子范围已知时用）
#   bash tools/check-docs-sync.sh                  # ③ 无参数：从 stdin 读 pre-push 钩子
#        #    格式（每行）：<local ref> <local oid> <remote ref> <remote oid>
#        #    无 stdin 时回退到 @{upstream}..HEAD
# =============================================================================

set -u

# 功能代码范围：命中任一模式即视为「功能变更」
CODE_PATTERNS='^(app/src/main/|app/build\.gradle\.kts$|build\.gradle\.kts$|settings\.gradle\.kts$|gradle\.properties$|gradle/libs\.versions\.toml$|app/proguard-rules\.pro$)'

# 需要保持同步的文档
DOCS='README.md TODO.md'

# 空树 hash（git 内部约定），用于首次提交/新分支时 diff 全部文件
EMPTY_TREE='4b825dc642cb6eb9a060e54bf8d69288fbee4904'

# ---------------------------------------------------------------------------
# 收集变更文件列表：输出到 stdout，一行一个
# ---------------------------------------------------------------------------
collect_changed_files() {
    local base="$1" head="$2"
    local base_oid head_oid

    # 全零 hash（首次 push / Actions 首次运行）→ 从空树开始算
    if [ "$base" = "0000000000000000000000000000000000000000" ] || [ -z "$base" ]; then
        base_oid="$EMPTY_TREE"
    else
        base_oid="$base"
    fi
    head_oid="$head"

    git diff --name-only "$base_oid" "$head_oid" 2>/dev/null
}

# ---------------------------------------------------------------------------
# 从 pre-push stdin 解析 commit 范围（多行，逐行取 remote_oid..local_oid）
# ---------------------------------------------------------------------------
collect_from_stdin() {
    # 依次尝试：stdin 有内容 → 用之；否则用 upstream..HEAD；再否则用最近一次提交
    if [ -p /dev/stdin ] || [ ! -t 0 ]; then
        # pre-push 钩子格式：<local ref> <local oid> <remote ref> <remote oid>
        local local_ref local_oid remote_ref remote_oid
        while read -r local_ref local_oid remote_ref remote_oid; do
            [ -z "$local_oid" ] && continue
            if [ "$remote_oid" = "0000000000000000000000000000000000000000" ]; then
                # 新分支/首次推送：diff 空树到 local_oid
                git diff --name-only "$EMPTY_TREE" "$local_oid" 2>/dev/null
            else
                git diff --name-only "$remote_oid" "$local_oid" 2>/dev/null
            fi
        done
    else
        # 无 stdin（手动运行）：用 upstream..HEAD
        if git rev-parse --verify --quiet @{upstream} >/dev/null 2>&1; then
            git diff --name-only "@{upstream}" HEAD 2>/dev/null
        else
            # 无 upstream（新仓库首次提交）：用最近一次提交
            git diff --name-only "$EMPTY_TREE" HEAD 2>/dev/null
        fi
    fi
}

# ---------------------------------------------------------------------------
# 主逻辑
# ---------------------------------------------------------------------------
main() {
    local base="${1:-}" head="${2:-}" files

    if [ -n "$base" ] && [ -n "$head" ]; then
        files="$(collect_changed_files "$base" "$head")"
    else
        files="$(collect_from_stdin)"
    fi

    # 无变更文件（例如纯 merge 无冲突提交）→ 无需检查
    [ -z "$files" ] && exit 0

    # 判定：是否涉及功能代码
    local has_code=false
    if echo "$files" | grep -E "$CODE_PATTERNS" >/dev/null 2>&1; then
        has_code=true
    fi

    # 判定：README.md / TODO.md 是否至少有一个同步更新
    local doc_changed=""
    for doc in $DOCS; do
        if echo "$files" | grep -x "$doc" >/dev/null 2>&1; then
            doc_changed="$doc_changed $doc"
        fi
    done

    if [ "$has_code" = "true" ] && [ -z "$doc_changed" ]; then
        echo ""
        echo "┌──────────────────────────────────────────────────────────────┐"
        echo "│ ⚠ 文档同步提醒：本次变更包含功能代码，但 README.md / TODO.md │"
        echo "│   均未同步更新。请确认是否需要：                              │"
        echo "│   · README.md —— 功能列表/技术栈是否过时                      │"
        echo "│   · TODO.md   —— 完成项是否勾选、新需求是否补充               │"
        echo "│   （提示仅供提醒，不阻断本次提交/推送）                        │"
        echo "└──────────────────────────────────────────────────────────────┘"
    fi

    exit 0
}

main "$@"
