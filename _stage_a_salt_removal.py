#!/usr/bin/env python3
"""Phase 4 / Stage A - finish removing upstream Salt from the leaf screens.

Token reads -> Miuix equivalents, dead Salt imports dropped, and the Salt
theme layer (ThemeSalt.kt + the LyricoSaltTheme wrapper) removed.
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

FILES = [
    'screens/AppLogScreen.kt',
    'screens/BatchTaskDetailScreen.kt',
    'screens/BatchTaskListScreen.kt',
    'ui/components/lyrics/LyricsTextStyle.kt',
]

# dead / unused upstream Salt imports
DROP_IMPORTS = {
    'screens/AppLogScreen.kt': ['import com.moriafly.salt.ui.SaltTheme'],
    'screens/BatchTaskDetailScreen.kt': ['import com.moriafly.salt.ui.SaltTheme'],
    'screens/BatchTaskListScreen.kt': ['import com.moriafly.salt.ui.SaltTheme'],
    'ui/components/lyrics/LyricsTextStyle.kt': ['import com.moriafly.salt.ui.SaltTheme'],
    'ui/components/lyrics/LyricsProcessingSheet.kt': ['import com.moriafly.salt.ui.Button'],
    'ui/components/poster/ArtistPosterPreview.kt': ['import com.moriafly.salt.ui.enabledAlpha'],
}

# fully-qualified Salt Text -> Miuix Text (Miuix Text already imported there)
FQ_TEXT = {'screens/BatchTaskListScreen.kt'}


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def ensure_miuix_theme(src):
    if 'import top.yukonga.miuix.kmp.theme.MiuixTheme' not in src:
        lines = src.split('\n')
        at = next((i for i, l in enumerate(lines) if l.startswith('import ')), len(lines))
        lines[at:at] = ['import top.yukonga.miuix.kmp.theme.MiuixTheme']
        src = '\n'.join(lines)
    return src


def main():
    for rel in FILES:
        p = os.path.join(ROOT, rel)
        src = read(p)
        orig = src
        for pat, rep in TOKENS:
            src = re.sub(pat, rep, src)
        if rel in FQ_TEXT:
            src = src.replace('com.moriafly.salt.ui.Text(', 'Text(')
        src = ensure_miuix_theme(src)
        for imp in DROP_IMPORTS.get(rel, []):
            src = src.replace(imp + '\n', '')
        if src != orig:
            write(p, src)
            print('  [updated] %s' % rel)

    # ---- SearchResultsScreen: the two salt branches are dead (salt == false) ----
    p = os.path.join(ROOT, 'screens/SearchResultsScreen.kt')
    src = read(p)
    before = src
    src = src.replace(
        '                salt && selected -> com.moriafly.salt.ui.SaltTheme.colors.onHighlight\n'
        '                salt -> com.moriafly.salt.ui.SaltTheme.colors.text\n', '')
    if src != before:
        write(p, src)
        print('  [updated] screens/SearchResultsScreen.kt (dead salt branches dropped)')
    else:
        print('  [WARN] SearchResultsScreen salt branches not matched - check manually')

    # ---- drop the Salt theme layer ----
    p = os.path.join(ROOT, 'ui/theme/ThemeSalt.kt')
    if os.path.exists(p):
        os.remove(p)
        print('  [deleted] ui/theme/ThemeSalt.kt')

    # ---- Theme.kt: unwrap the LyricoSaltTheme layer ----
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
        print('  [WARN] Theme.kt pattern not matched - check manually')

    print('\n=== remaining upstream Salt references ===')
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if not f.endswith('.kt'):
                continue
            p = os.path.join(dp, f)
            for i, line in enumerate(read(p).split('\n'), 1):
                if re.search(r'com\.moriafly\.salt|\bSaltTheme\b|lyrico\.salt', line):
                    rel = os.path.relpath(p, ROOT).replace('\\', '/')
                    print('  %s:%d  %s' % (rel, i, line.strip()[:100]))


if __name__ == '__main__':
    main()
