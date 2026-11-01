#!/usr/bin/env python3
"""Remove the 3 dead `val isSaltEngine = false` leftovers, then drop imports
that are no longer referenced (line-based, so nothing else can be clobbered).
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'

IMPORT_RE = re.compile(r'^import\s+([\w.]+)(?:\s+as\s+(\w+))?\s*$')


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def drop_is_salt_engine():
    for rel in ('screens/library/AlbumsPage.kt',
                'screens/library/ArtistsPage.kt',
                'screens/library/SongsPage.kt'):
        p = os.path.join(ROOT, rel)
        src = read(p)
        new = re.sub(r'^[ \t]*val\s+isSaltEngine\s*=\s*false[ \t]*\n', '', src, flags=re.M)
        if new != src:
            write(p, new)
            print('  [removed] %s :: val isSaltEngine' % rel)


def drop_unused_imports():
    removed = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            src = read(p)
            lines = src.split('\n')
            import_idx = [i for i, l in enumerate(lines) if IMPORT_RE.match(l.strip()) and l.startswith('import')]
            if not import_idx:
                continue
            body = '\n'.join(l for i, l in enumerate(lines) if i not in import_idx)
            drop = []
            for i in import_idx:
                m = IMPORT_RE.match(lines[i].strip())
                sym = m.group(2) or m.group(1).split('.')[-1]
                if sym == '*':
                    continue
                # operator imports for `by` delegation: the symbol never appears in
                # the source text but is still required at compile time
                if m.group(1).startswith('androidx.compose.runtime.') and sym in ('getValue', 'setValue'):
                    continue
                if not re.search(r'\b%s\b' % re.escape(sym), body):
                    drop.append(i)
            if drop:
                for i in reversed(drop):
                    removed += 1
                    print('  [imp-] %s : %s' % (os.path.relpath(p, ROOT).replace('\\', '/'),
                                               lines[i].strip()[:80]))
                lines = [l for i, l in enumerate(lines) if i not in drop]
                write(p, '\n'.join(lines))
    print('\n=== unused imports removed: %d ===' % removed)


if __name__ == '__main__':
    drop_is_salt_engine()
    print()
    drop_unused_imports()
