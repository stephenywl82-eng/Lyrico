#!/usr/bin/env python3
"""Restore the `actions = { ... }` block that was wrongly removed from inside
each `miuixTopBar` (the SmallTopAppBar's real action buttons).

The earlier removal loop deleted the FIRST `actions = {` after each
LibraryDetailPage( call, then deleted the next one too - which was the
SmallTopAppBar's own `actions = { ... }` living inside `miuixTopBar`.
The original text is taken verbatim from the pre-Phase-3 backup and re-inserted
right after the `navigationIcon = { ... },` block of the same top bar.
"""
import os
import re

SRC = 'lyrico-app/src/main/java/com/lonx/lyrico'
BACKUP = 'D:/tmp/lyrico_src_backup/java/com/lonx/lyrico'

FILES = [
    'screens/AlbumDetailScreen.kt',
    'screens/AppLogScreen.kt',
    'screens/ArtistPosterFoldersScreen.kt',
    'screens/BatchEditScreen.kt',
    'screens/BatchRenameScreen.kt',
    'screens/BatchTaskDetailScreen.kt',
    'screens/BatchTaskListScreen.kt',
    'screens/EditFieldSettingsScreen.kt',
    'screens/EditMetadataScreen.kt',
    'screens/FolderManagerScreen.kt',
    'screens/PluginConfigScreen.kt',
    'screens/PluginManagerScreen.kt',
]


def match_brace(s, i):
    d = 0
    j = i
    while j < len(s):
        if s[j] == '{':
            d += 1
        elif s[j] == '}':
            d -= 1
            if d == 0:
                return j
        j += 1
    return -1


def find_actions_block(s, start):
    """find `actions = {` at/after `start`, return (block_text, end_index)"""
    m = re.search(r'\n([ \t]*)actions = \{', s[start:])
    if not m:
        return None, -1
    i = start + m.end() - 1
    j = match_brace(s, i)
    if j < 0:
        return None, -1
    return s[m.start() + start: j + 1], j + 1


def main():
    for rel in FILES:
        p = os.path.join(SRC, rel)
        bk = os.path.join(BACKUP, rel)
        if not os.path.exists(bk) or not os.path.exists(p):
            print('  [SKIP] %s' % rel)
            continue
        cur = open(p, encoding='utf-8').read()
        old = open(bk, encoding='utf-8').read()

        # in the backup: take the `actions = {` that lives inside miuixTopBar
        mt = old.find('miuixTopBar = {')
        if mt < 0:
            print('  [SKIP] %s (no miuixTopBar in backup)' % rel)
            continue
        block, _ = find_actions_block(old, mt)
        if not block:
            print('  [SKIP] %s (no actions block in backup miuixTopBar)' % rel)
            continue

        # in the current file: find miuixTopBar, then its navigationIcon block
        cmt = cur.find('miuixTopBar = {')
        if cmt < 0:
            print('  [SKIP] %s (no miuixTopBar now)' % rel)
            continue
        nm = re.search(r'\n([ \t]*)navigationIcon = \{', cur[cmt:])
        if not nm:
            print('  [WARN] %s: no navigationIcon to anchor after' % rel)
            continue
        ni = cmt + nm.end() - 1
        nj = match_brace(cur, ni)
        if nj < 0:
            print('  [WARN] %s: navigationIcon brace mismatch' % rel)
            continue
        # after the navigationIcon block there should be a `},`
        k = nj + 1
        while k < len(cur) and cur[k] in ' \t':
            k += 1
        if cur[k] == ',':
            k += 1
        new = cur[:k] + '\n' + block.strip('\n') + cur[k:]
        open(p, 'w', encoding='utf-8').write(new)
        print('  [restored] %-42s actions block (%d lines)' % (rel, block.count('\n') + 1))


if __name__ == '__main__':
    main()
