#!/usr/bin/env python3
"""Restore the androidx.compose.runtime getValue/setValue operator imports.

They are needed for `val x by <State>` delegation even though the symbol name
never appears in the source text, so a text-based "unused import" sweep wrongly
dropped them. The backup holds the known-good import state -> restore from it.
"""
import os
import re

SRC = 'lyrico-app/src/main/java/com/lonx/lyrico'
BACKUP = 'D:/tmp/lyrico_src_backup/java/com/lonx/lyrico'
WANT = ('import androidx.compose.runtime.getValue',
        'import androidx.compose.runtime.setValue')


def main():
    restored = 0
    for dp, dn, fn in os.walk(SRC):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            rel = os.path.relpath(p, SRC).replace('\\', '/')
            bk = os.path.join(BACKUP, rel)
            if not os.path.exists(bk):
                continue
            src = open(p, encoding='utf-8').read()
            bsrc = open(bk, encoding='utf-8').read()
            want = [w for w in WANT if re.search(r'^%s$' % re.escape(w), bsrc, re.M)]
            have = [w for w in WANT if re.search(r'^%s$' % re.escape(w), src, re.M)]
            missing = [w for w in want if w not in have]
            if not missing:
                continue
            lines = src.split('\n')
            # place them next to the other androidx.compose.runtime imports
            idxs = [i for i, l in enumerate(lines)
                    if l.startswith('import androidx.compose.runtime.')]
            if idxs:
                at = max(idxs) + 1
            else:
                at = next((i for i, l in enumerate(lines) if l.startswith('import ')), 0)
            lines[at:at] = missing
            open(p, 'w', encoding='utf-8').write('\n'.join(lines))
            restored += len(missing)
            print('  [restored] %-58s %s' % (rel, ', '.join(m.split('.')[-1] for m in missing)))
    print('\n=== operator imports restored: %d ===' % restored)


if __name__ == '__main__':
    main()
