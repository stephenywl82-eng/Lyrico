#!/usr/bin/env python3
"""Phase 4 / Stage B - final Salt removal.

  1. LibraryDetailPage ignores its `actions` argument (it renders `miuixTopBar`
     only), so every `actions = { ... }` block is dead at runtime -> drop them
     and the parameter itself.
  2. Token reads -> Miuix equivalents; Salt theme layer deleted.
  3. With no upstream Salt API left, drop the @file:OptIn(UnstableSaltUiApi).
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'

TOKENS = [
    (r'SaltTheme\.colors\.highlight', 'MiuixTheme.colorScheme.primary'),
    (r'SaltTheme\.colors\.error', 'MiuixTheme.colorScheme.error'),
    (r'SaltTheme\.colors\.subText', 'MiuixTheme.colorScheme.onSurfaceVariantSummary'),
    (r'SaltTheme\.colors\.onHighlight', 'MiuixTheme.colorScheme.onPrimary'),
    (r'SaltTheme\.colors\.text', 'MiuixTheme.colorScheme.onSurface'),
    (r'SaltTheme\.textStyles\.main', 'MiuixTheme.textStyles.main'),
    (r'SaltTheme\.textStyles\.sub\b', 'MiuixTheme.textStyles.body2'),
]


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def strip_actions_args(src):
    """remove the `actions = { ... }` argument of every LibraryDetailPage(...) call"""
    out = src
    changed = 0
    while True:
        m = re.search(r'LibraryDetailPage\(', out)
        hit = False
        while m:
            seg = out[m.end():]
            am = re.search(r'\n(\s*)actions\s*=\s*\{', seg)
            if not am:
                break
            # brace-match the actions block
            i = m.end() + am.end() - 1
            d = 0
            j = i
            while j < len(out):
                if out[j] == '{':
                    d += 1
                elif out[j] == '}':
                    d -= 1
                    if d == 0:
                        break
                j += 1
            # also swallow a trailing comma right after the block
            end = j + 1
            while end < len(out) and out[end] in ' \t':
                end += 1
            if end < len(out) and out[end] == ',':
                end += 1
            out = out[:m.end() + am.start()] + out[end:]
            changed += 1
            hit = True
            m = re.search(r'LibraryDetailPage\(', out)
        if not hit:
            break
    return out, changed


def main():
    # ---- 1. drop the dead `actions` blocks -------------------------------------
    total = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            src = read(p)
            if 'LibraryDetailPage(' not in src:
                continue
            new, n = strip_actions_args(src)
            if n:
                write(p, new)
                total += n
                print('  [actions-] %-50s -%d block(s)' % (os.path.relpath(p, ROOT).replace('\\', '/'), n))
    print('  dead actions blocks removed: %d' % total)

    # ---- 1b. drop the `actions` parameter from LibraryDetailPage ---------------
    p = os.path.join(ROOT, 'ui/components/library/LibraryDetailPresentation.kt')
    src = read(p)
    new = src.replace('    actions: @Composable () -> Unit = {},\n', '')
    if new != src:
        write(p, new)
        print('  [updated] LibraryDetailPage: `actions` parameter removed')

    # ---- 2. tokens + Salt theme layer -----------------------------------------
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            src = read(p)
            if 'SaltTheme' not in src and 'com.moriafly.salt' not in src:
                continue
            new = src
            for pat, rep in TOKENS:
                new = re.sub(pat, rep, new)
            # ensure MiuixTheme is imported where we now use it
            if 'MiuixTheme.' in new and 'import top.yukonga.miuix.kmp.theme.MiuixTheme' not in new:
                lines = new.split('\n')
                at = next((i for i, l in enumerate(lines) if l.startswith('import ')), len(lines))
                lines[at:at] = ['import top.yukonga.miuix.kmp.theme.MiuixTheme']
                new = '\n'.join(lines)
            if new != src:
                write(p, new)
                print('  [tokens] %s' % os.path.relpath(p, ROOT).replace('\\', '/'))

    # ---- 3. drop the Salt theme layer ------------------------------------------
    p = os.path.join(ROOT, 'ui/theme/ThemeSalt.kt')
    if os.path.exists(p):
        os.remove(p)
        print('  [deleted] ui/theme/ThemeSalt.kt')

    p = os.path.join(ROOT, 'ui/theme/Theme.kt')
    src = read(p)
    new = src.replace(
        '        LyricoSaltTheme(colorMode, MiuixTheme.colorScheme.primary, monetEnabled, indication) {\n'
        '            LyricoSdwTheme(colorMode) {\n'
        '                CompositionLocalProvider(\n'
        '                    LocalIndication provides indication,\n'
        '                    content = content,\n'
        '                )\n'
        '            }\n'
        '        }\n',
        '        LyricoSdwTheme(colorMode) {\n'
        '            CompositionLocalProvider(\n'
        '                LocalIndication provides indication,\n'
        '                content = content,\n'
        '            )\n'
        '        }\n')
    if new != src:
        new = new.replace('import top.yukonga.miuix.kmp.theme.MiuixTheme\n', '')
        write(p, new)
        print('  [updated] ui/theme/Theme.kt (LyricoSaltTheme layer removed)')
    else:
        print('  [WARN] Theme.kt pattern not matched')

    # ---- 4. drop the @file:OptIn(UnstableSaltUiApi) everywhere ----------------
    n = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            src = read(p)
            new = re.sub(r'^@file:OptIn\(com\.moriafly\.salt\.ui\.UnstableSaltUiApi::class\)\n', '',
                         src, flags=re.M)
            if new != src:
                write(p, new)
                n += 1
    print('  @file:OptIn(UnstableSaltUiApi) removed from %d files' % n)

    print('\n=== remaining upstream Salt references ===')
    hits = 0
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            for i, line in enumerate(read(p).split('\n'), 1):
                if re.search(r'com\.moriafly\.salt|\bSaltTheme\b|LyricoSaltTheme', line):
                    print('  %s:%d  %s' % (os.path.relpath(p, ROOT).replace('\\', '/'), i, line.strip()[:100]))
                    hits += 1
    print('  total: %d' % hits)


if __name__ == '__main__':
    main()
