#!/usr/bin/env python3
"""Repair import lines mangled by the migration script.

The script removed upstream Salt imports with a blind str.replace, which also
stripped the prefix from longer imports (e.g. removing `import ...salt.ui.Item`
also gutted `import ...salt.ui.ItemSwitcher` -> `Switcher`).

This restores each damaged line by looking up the ORIGINAL import (which still
exists in the pre-Phase-3 backup) that ends with the leftover tail.
"""
import os
import re
import sys

SRC = 'lyrico-app/src/main/java/com/lonx/lyrico'
BACKUP = 'D:/tmp/lyrico_src_backup/java/com/lonx/lyrico'

BARE = re.compile(r'^[A-Za-z_][A-Za-z0-9_]*$')
UPSTREAM = re.compile(r'^\s*import\s+(com\.moriafly\.salt\.ui\.[A-Za-z0-9_]+)\s*$', re.M)


def backup_imports(rel):
    p = os.path.join(BACKUP, rel)
    if not os.path.exists(p):
        return []
    src = open(p, encoding='utf-8').read()
    return [m.group(1) for m in UPSTREAM.finditer(src)]


def main():
    fixed = 0
    for dirpath, dirnames, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith('.kt'):
                continue
            p = os.path.join(dirpath, fn)
            rel = os.path.relpath(p, SRC).replace('\\', '/')
            lines = open(p, encoding='utf-8').read().split('\n')
            orig_imports = backup_imports(rel)
            changed = False
            for i, line in enumerate(lines):
                s = line.strip()
                if not s or not BARE.match(s):
                    continue
                # candidate original imports ending with this tail
                cands = [imp for imp in orig_imports if imp.split('.')[-1].endswith(s)]
                if len(cands) == 1:
                    lines[i] = 'import ' + cands[0]
                    changed = True
                    print('  [fix] %s:%d  ->  import %s' % (rel, i + 1, cands[0]))
                elif len(cands) > 1:
                    print('  [AMBIGUOUS] %s:%d  tail=%s cands=%s' % (rel, i + 1, s, cands))
                else:
                    print('  [NO MATCH] %s:%d  tail=%s' % (rel, i + 1, s))
            if changed:
                open(p, 'w', encoding='utf-8').write('\n'.join(lines))
                fixed += 1
    print('\n=== files repaired: %d ===' % fixed)


if __name__ == '__main__':
    main()
