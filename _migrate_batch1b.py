#!/usr/bin/env python3
"""Phase 4 / Batch 1b - migrate the editor + batch-edit screens off Salt.

  * EditorSalt.kt (SaltEditorField)  ->  sdw/SdwEditorField.kt (SdwEditorField)
  * SaltText / RoundedColumn / SaltTheme tokens  ->  Miuix equivalents
  * drop the dead SaltEditorUndoAction + unused imports
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'

EDITS = {
    'screens/BatchEditScreen.kt': [
        # imports
        ('import com.lonx.lyrico.ui.components.editor.SaltEditorField',
         'import com.lonx.lyrico.ui.components.sdw.SdwEditorField'),
        ('import com.moriafly.salt.ui.RoundedColumn\n', ''),
        ('import com.moriafly.salt.ui.ItemOuterTitle\n', ''),
        ('import com.moriafly.salt.ui.Text as SaltText\n', ''),
        ('import com.moriafly.salt.ui.SaltTheme\n', ''),
        # components
        (r'\bSaltEditorField\b', 'SdwEditorField'),
        (r'\bSaltText\b', 'Text'),
        (r'RoundedColumn\(paddingValues = PaddingValues\(0\.dp\)\)', 'Card'),
        (r'\bRoundedColumn\b', 'Card'),
        # tokens
        (r'SaltTheme\.colors\.highlight', 'MiuixTheme.colorScheme.primary'),
        (r'SaltTheme\.colors\.error', 'MiuixTheme.colorScheme.error'),
        (r'SaltTheme\.colors\.subText', 'MiuixTheme.colorScheme.onSurfaceVariantSummary'),
        (r'SaltTheme\.textStyles\.sub\b', 'MiuixTheme.textStyles.body2'),
    ],
    'screens/EditMetadataScreen.kt': [
        ('import com.lonx.lyrico.ui.components.editor.SaltEditorField\n', ''),
        ('import com.lonx.lyrico.ui.components.editor.SaltEditorUndoAction\n', ''),
        ('import com.moriafly.salt.ui.SaltTheme\n', ''),
        (r'SaltTheme\.colors\.highlight', 'MiuixTheme.colorScheme.primary'),
    ],
}


def main():
    for rel, rules in EDITS.items():
        p = os.path.join(ROOT, rel)
        src = open(p, encoding='utf-8').read()
        before = src
        for pat, rep in rules:
            if pat.startswith('import '):
                if pat not in src:
                    print('  [SKIP] %s : %s' % (rel, pat.strip()))
                    continue
                src = src.replace(pat, rep)
            else:
                src = re.sub(pat, rep, src)
        if src != before:
            open(p, 'w', encoding='utf-8').write(src)
            print('  [updated] %s' % rel)
        else:
            print('  [no change] %s' % rel)
    # report leftovers
    print('\n=== leftover Salt refs in the two screens ===')
    for rel in EDITS:
        p = os.path.join(ROOT, rel)
        for i, line in enumerate(open(p, encoding='utf-8'), 1):
            if re.search(r'\bSalt\w*|moriafly\.salt', line):
                print('  %s:%d  %s' % (rel, i, line.rstrip()[:110]))


if __name__ == '__main__':
    main()
