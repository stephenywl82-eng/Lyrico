#!/usr/bin/env python3
"""Phase 4 / Batch 1 - migrate the 11 shared Salt wrappers to their SDW equivalents.

Mapping is 1:1 (SDW components already exist). For every consumer file:
  * import com.lonx.lyrico.ui.components.salt.SaltX  ->  ...ui.components.sdw.SdwX
  * identifier SaltX                                   ->  SdwX
Then drop upstream `com.moriafly.salt.ui.*` imports that became unused.

Files under ui/components/salt/ are skipped here (they are deleted afterwards).
"""
import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                    'lyrico-app', 'src', 'main', 'java', 'com', 'lonx', 'lyrico')
SALT_DIR = os.path.join(ROOT, 'ui', 'components', 'salt')

MAP = {
    'SaltBasicDialog': 'SdwBasicDialog',
    'SaltDialogTitle': 'SdwDialogTitle',
    'SaltDialogButton': 'SdwDialogButton',
    'SaltBottomSheet': 'SdwBottomSheet',
    'SaltConfirmDialog': 'SdwConfirmDialog',
    'SaltDropdownRow': 'SdwDropdownRow',
    'SaltIconButton': 'SdwIconButton',
    'SaltInputDialog': 'SdwInputDialog',
    'SaltMenuBottomSheet': 'SdwMenuBottomSheet',
    'SaltSheetActions': 'SdwSheetActions',
    'SaltTextInput': 'SdwTextInput',
}
# longest names first so no name is a prefix of another
ORDER = sorted(MAP, key=len, reverse=True)

UPSTREAM_IMPORT = re.compile(
    r'^\s*import\s+com\.moriafly\.salt\.ui\.([A-Za-z0-9_]+)\s*$', re.M)


def migrate(path):
    with open(path, encoding='utf-8') as f:
        src = f.read()
    orig = src
    if 'com.lonx.lyrico.ui.components.salt' not in src:
        return None

    # 1. rewrite imports + identifiers
    for s in ORDER:
        d = MAP[s]
        src = re.sub(r'import\s+com\.lonx\.lyrico\.ui\.components\.salt\.%s\b'
                      % s,
                      'import com.lonx.lyrico.ui.components.sdw.%s' % d, src)
        src = re.sub(r'\b%s\b' % s, d, src)

    # 2. drop upstream Salt imports that are no longer referenced
    body = UPSTREAM_IMPORT.sub('', src)
    for m in UPSTREAM_IMPORT.finditer(src):
        sym = m.group(1)
        rest = UPSTREAM_IMPORT.sub('', src)
        if not re.search(r'\b%s\b' % sym, rest):
            src = src.replace(m.group(0), '')

    if src != orig:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(src)
        return src
    return None


def main():
    changed = []
    for dirpath, dirnames, filenames in os.walk(ROOT):
        if os.path.abspath(dirpath).startswith(os.path.abspath(SALT_DIR)):
            continue
        for fn in filenames:
            if not fn.endswith('.kt'):
                continue
            p = os.path.join(dirpath, fn)
            if migrate(p):
                changed.append(os.path.relpath(p, ROOT).replace('\\', '/'))
    for c in sorted(changed):
        print('  [migrated]', c)
    print('\n=== files migrated: %d ===' % len(changed))


if __name__ == '__main__':
    main()
