#!/usr/bin/env python3
"""Phase 4 / Stage C - last Salt references.

  * fully-qualified Salt Text -> Miuix Text
  * drop the two dead `salt` branches in SearchResultsScreen (salt == false)
  * strip UnstableSaltUiApi out of every @file:OptIn (all forms)
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'
SALT_OPTIN = 'com.moriafly.salt.ui.UnstableSaltUiApi::class'


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def fix_optin(m):
    inner = m.group(1)
    parts = [p.strip() for p in inner.split(',') if p.strip()]
    parts = [p for p in parts if p != SALT_OPTIN]
    if not parts:
        return ''
    return '@file:OptIn(%s)' % ', '.join(parts)


def main():
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            src = read(p)
            new = src
            # fully-qualified Salt Text -> Miuix Text
            new = new.replace('com.moriafly.salt.ui.Text(', 'Text(')
            # dead salt branches in SearchResultsScreen
            new = new.replace(
                '                salt && selected -> com.moriafly.salt.ui.MiuixTheme.colorScheme.onPrimary\n'
                '                salt -> com.moriafly.salt.ui.MiuixTheme.colorScheme.onSurface\n', '')
            # strip UnstableSaltUiApi from every @file:OptIn form
            new = re.sub(r'@file:OptIn\(([^)]*)\)', fix_optin, new, flags=re.S)
            # drop a @file:OptIn that became empty
            new = re.sub(r'^@file:OptIn\(\)\n', '', new, flags=re.M)
            if new != src:
                write(p, new)
                print('  [fixed] %s' % os.path.relpath(p, ROOT).replace('\\', '/'))

    print('\n=== remaining upstream Salt references ===')
    hits = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            for i, line in enumerate(read(p).split('\n'), 1):
                if re.search(r'com\.moriafly\.salt|\bSaltTheme\b|LyricoSaltTheme|UnstableSaltUiApi', line):
                    print('  %s:%d  %s' % (os.path.relpath(p, ROOT).replace('\\', '/'), i, line.strip()[:100]))
                    hits += 1
    print('  total: %d' % hits)


if __name__ == '__main__':
    main()
