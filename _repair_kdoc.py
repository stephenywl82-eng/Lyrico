#!/usr/bin/env python3
"""Re-insert the '/**' opener of KDoc blocks whose opener line was lost.

A KDoc body looks like
    /**
     * text
     */
When the opener was dropped only ' * text' and ' */' remain, which is a Kotlin
syntax error. Detect such orphans (a ' * ...' line that is not preceded by a
'/**' and not inside an already-open block) and restore the opener.
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'
STAR = re.compile(r'^(\s*)\*')


def fix(path):
    src = open(path, encoding='utf-8').read()
    lines = src.split('\n')
    out = []
    in_kdoc = False
    added = 0
    for line in lines:
        m = STAR.match(line)
        if m:
            star_col = len(m.group(1))
            if not in_kdoc:
                prev = out[-1].strip() if out else ''
                if not prev.startswith('/**'):
                    out.append(' ' * max(star_col - 1, 0) + '/**')
                    added += 1
            in_kdoc = (line.strip() != '*/')
        else:
            in_kdoc = False
        out.append(line)
    if added:
        open(path, 'w', encoding='utf-8').write('\n'.join(out))
    return added


def main():
    total = 0
    files = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            n = fix(p)
            if n:
                files += 1
                total += n
                print('  [kdoc] %-55s +%d' % (os.path.relpath(p, ROOT).replace('\\', '/'), n))
    print('\n=== KDoc openers restored: %d in %d files ===' % (total, files))


if __name__ == '__main__':
    main()
