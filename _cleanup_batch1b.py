#!/usr/bin/env python3
"""Phase 4 / Batch 1b cleanup - drop the dead Salt code in the two screens.

  * BatchEditScreen: SaltBatchField + SaltBatchOverlays are dead (0 call sites)
    and run to EOF -> truncate. That also removes the last live uses of
    SaltEditorField, which makes ui/components/editor/EditorSalt.kt dead too.
  * remove the now-pointless @file:OptIn(UnstableSaltUiApi) from both files.
"""
import os
import re

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def main():
    # ---- BatchEditScreen: truncate the two dead trailing functions ----
    p = os.path.join(ROOT, 'screens/BatchEditScreen.kt')
    lines = read(p).split('\n')
    # find the declaration line of SaltBatchField
    cut = None
    for i, l in enumerate(lines):
        if l.startswith('private fun SaltBatchField('):
            # walk back over the @Composable annotation + blank line
            cut = i
            while cut > 0 and lines[cut - 1].strip() in ('', '@Composable'):
                cut -= 1
            break
    assert cut is not None, 'SaltBatchField not found'
    kept = lines[:cut]
    while kept and kept[-1].strip() == '':
        kept.pop()
    src = '\n'.join(kept) + '\n'
    # now-unused imports
    for imp in ('import com.lonx.lyrico.ui.components.sdw.SdwEditorField\n',
                '@file:OptIn(com.moriafly.salt.ui.UnstableSaltUiApi::class)\n\n'):
        src = src.replace(imp, '')
    write(p, src)
    print('  [truncated] BatchEditScreen.kt  removed lines %d-%d (dead SaltBatchField/SaltBatchOverlays)'
          % (cut + 1, len(lines)))

    # ---- EditMetadataScreen: drop the Salt opt-in if nothing Salt is left ----
    p = os.path.join(ROOT, 'screens/EditMetadataScreen.kt')
    src = read(p)
    body = re.sub(r'^@file:OptIn\(com\.moriafly\.salt\.ui\.UnstableSaltUiApi::class\)\n', '', src,
                  flags=re.M)
    if re.search(r'com\.moriafly\.salt', body):
        print('  [keep opt-in] EditMetadataScreen.kt still uses upstream Salt')
    else:
        src = body.replace('\n\n\n', '\n\n', 1) if body.startswith('\n') else body
        write(p, src)
        print('  [updated] EditMetadataScreen.kt - Salt opt-in removed')

    # ---- EditorSalt.kt is now dead -> delete ----
    p = os.path.join(ROOT, 'ui/components/editor/EditorSalt.kt')
    if os.path.exists(p):
        os.remove(p)
        print('  [deleted] ui/components/editor/EditorSalt.kt (0 remaining users)')

    print('\n=== remaining Salt refs in the two screens ===')
    for rel in ('screens/BatchEditScreen.kt', 'screens/EditMetadataScreen.kt'):
        p = os.path.join(ROOT, rel)
        for i, line in enumerate(read(p).split('\n'), 1):
            if re.search(r'\bSalt\w*|moriafly\.salt', line):
                print('  %s:%d  %s' % (rel, i, line.strip()[:110]))


if __name__ == '__main__':
    main()
