#!/usr/bin/env python3
"""Repair the annotation/KDoc damage left by the dead-code sweep.

The earlier removal cut the region starting *inside* the annotation, leaving
behind a stray '@' plus the doc-comment of the deleted function. This walks each
file and reconstructs the correct shape:

  * '@@X'                -> '@X'            (one stray '@')
  * '@/** doc */'        -> '/** doc */'    (stray '@' + a real doc)
  * a lone '@'           -> removed
  * doc-comments that documented a deleted function -> removed
  * the doc that belongs to the next declaration -> kept
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'

KDOC_START = re.compile(r'^\s*/\*\*')
DECL = re.compile(r'^\s*(?:@\w+(?:\([^)]*\))?\s*)*(?:internal |private |public |override |suspend |inline |operator |infix |external )*'
                   r'(?:fun|val|var|class|object|interface|enum class|data class|typealias|companion)\b')


def repair(path):
    src = open(path, encoding='utf-8').read()
    lines = src.split('\n')
    out = []
    pending = []      # indices in `out` of candidate doc-comments
    orphan = set()

    def orphan_all():
        for i in pending:
            orphan.add(i)
        pending.clear()

    for line in lines:
        if re.match(r'^@@\w', line):
            orphan_all()
            out.append(line[1:])
            continue
        if re.match(r'^@/\*\*', line):
            orphan_all()
            out.append(line[1:])
            pending.append(len(out) - 1)
            continue
        if line.strip() == '@':
            orphan_all()
            continue
        if KDOC_START.match(line):
            out.append(line)
            pending.append(len(out) - 1)
            continue
        if DECL.match(line):
            # only the last pending doc belongs to this declaration
            if len(pending) > 1:
                for i in pending[:-1]:
                    orphan.add(i)
            pending = pending[-1:]
            out.append(line)
            continue
        out.append(line)

    if len(pending) > 1:
        for i in pending[:-1]:
            orphan.add(i)

    new_lines = [l for i, l in enumerate(out) if i not in orphan]
    # collapse 3+ blank lines into one
    cleaned = []
    for l in new_lines:
        if l.strip() == '' and cleaned and cleaned[-1].strip() == '':
            continue
        cleaned.append(l)
    new = '\n'.join(cleaned)
    if new != src:
        open(path, 'w', encoding='utf-8').write(new)
        return True
    return False


def main():
    n = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            if repair(p):
                n += 1
                print('  [repaired] %s' % os.path.relpath(p, ROOT).replace('\\', '/'))
    print('\n=== files repaired: %d ===' % n)
    print('\n=== remaining stray annotations ===')
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            for i, line in enumerate(open(p, encoding='utf-8'), 1):
                if re.match(r'^@@?\w*\s*$', line) or re.match(r'^@/\*\*', line):
                    print('  %s:%d  %s' % (os.path.relpath(p, ROOT).replace('\\', '/'), i, line.rstrip()[:80]))


if __name__ == '__main__':
    main()
