#!/usr/bin/env python3
"""
修复 12 个文件里被恢复脚本插回的 miuixTopBar actions 块：闭合 '}' 后缺少逗号，
导致紧随其后的 `scrollBehavior = ...` 被解析进 actions lambda 内部。

错误形态（每个文件 5 个错）：
  - @Composable invocations can only happen from the context of a @Composable function
  - Unresolved reference 'scrollBehavior' on receiver of type '() -> Unit'
  - Syntax error: Expecting an element.  (x2)

修法：给 actions 块的闭合 '}' 补上 ','
"""
import os
import re
import sys

BASE = r"E:\WorkBuddy\Claw\Lyrico\lyrico-app\src\main\java\com\lonx\lyrico\screens"

FILES = [
    "AlbumDetailScreen.kt",
    "AppLogScreen.kt",
    "ArtistPosterFoldersScreen.kt",
    "BatchEditScreen.kt",
    "BatchRenameScreen.kt",
    "BatchTaskDetailScreen.kt",
    "BatchTaskListScreen.kt",
    "EditFieldSettingsScreen.kt",
    "EditMetadataScreen.kt",
    "FolderManagerScreen.kt",
    "PluginConfigScreen.kt",
    "PluginManagerScreen.kt",
]

ACTIONS_RE = re.compile(r"^\s*actions\s*=\s*\{\s*$")


def strip_literal(line):
    """粗略去掉字符串/字符字面量里的花括号，避免计数被 '${...}' 之类的干扰。"""
    out = []
    in_s = False
    i = 0
    while i < len(line):
        c = line[i]
        if c == '"':
            # 处理转义
            if in_s and i > 0 and line[i - 1] == "\\":
                out.append(c)
            else:
                in_s = not in_s
        elif not in_s:
            out.append(c)
        i += 1
    return "".join(out)


def find_actions_blocks(lines):
    """返回 [(open_idx, close_idx)] —— actions = { ... } 的起止行下标(0-based)。"""
    blocks = []
    for i, raw in enumerate(lines):
        if not ACTIONS_RE.match(raw):
            continue
        depth = 0
        for j in range(i, len(lines)):
            code = strip_literal(lines[j])
            # 去掉行注释
            code = re.sub(r"//.*$", "", code)
            for ch in code:
                if ch == "{":
                    depth += 1
                elif ch == "}":
                    depth -= 1
            if depth == 0 and j >= i:
                blocks.append((i, j))
                break
    return blocks


def fix_file(path, apply):
    with open(path, encoding="utf-8") as f:
        lines = f.read().split("\n")

    changes = []
    for open_idx, close_idx in find_actions_blocks(lines):
        close_line = lines[close_idx]
        if close_line.rstrip().endswith(","):
            continue  # 已经有逗号，正常
        # 找下一个非空行
        nxt = close_idx + 1
        while nxt < len(lines) and lines[nxt].strip() == "":
            nxt += 1
        if nxt >= len(lines):
            continue
        if "scrollBehavior" not in lines[nxt]:
            continue  # 下一项不是 scrollBehavior，说明不是我们要修的那处
        changes.append((close_idx, close_line, lines[nxt].strip()[:60]))

    if apply:
        for close_idx, old, _ in changes:
            lines[close_idx] = old.rstrip() + ","
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.write("\n".join(lines))

    return changes


def main():
    apply = "--apply" in sys.argv
    total = 0
    for name in FILES:
        p = os.path.join(BASE, name)
        if not os.path.exists(p):
            print(f"[缺失] {name}")
            continue
        changes = fix_file(p, apply)
        total += len(changes)
        tag = "已修" if apply else "待修"
        if changes:
            for idx, old, nxt in changes:
                print(f"[{tag}] {name}:{idx+1}  '{old.strip()}'  ->  '{old.strip()},'   (下一行: {nxt})")
        else:
            print(f"[ok ] {name} 无需修改")
    print(f"\n合计 {total} 处 {'已应用' if apply else '(dry-run，未写盘)'}")


if __name__ == "__main__":
    main()
