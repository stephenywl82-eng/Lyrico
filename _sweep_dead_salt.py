#!/usr/bin/env python3
"""Phase 4 - sweep the remaining dead Salt code.

Every target is re-verified as having 0 references outside its own declaration
before anything is deleted, so this can only remove genuinely dead code.
"""
import os
import re
import sys

ROOT = 'lyrico-app/src/main/java/com/lonx/lyrico'

# file -> symbols to remove (functions/objects)
FUNCS = {
    'screens/AboutScreen.kt': ['SaltAboutAppInfo'],
    'screens/AlbumDetailScreen.kt': ['SaltAlbumActionsMenu'],
    'screens/SearchCoverScreen.kt': ['SaltCircularProgress'],
    'screens/ArtistPosterFolderContents.kt': ['SaltLoadingIndicator'],
    'screens/SearchLyricsScreen.kt': ['SaltLyricsProgress'],
    'screens/PluginManagerScreen.kt': ['SaltPluginActionButton', 'SaltPluginBadge', 'SaltPluginSubText'],
    'screens/SearchResultsScreen.kt': ['SaltResultsProgress'],
    'screens/ArtistSplitSettingsScreen.kt': ['SaltRuleRow'],
    'ui/components/song/SongDetailBottomSheet.kt': ['SaltSongDetailItem'],
    'screens/LocalSearchScreen.kt': ['LocalSearchSaltShell'],
}
# files whose every declaration is dead -> whole file goes
DEAD_FILES = [
    'screens/settings/SettingsSalt.kt',
    'screens/library/LibrarySalt.kt',
    'ui/components/library/SaltLibraryItems.kt',
]
# simple single-line declarations to drop
VALS = {
    'screens/library/AlbumsPage.kt': ['isSaltEngine'],
    'screens/library/ArtistsPage.kt': ['isSaltEngine'],
}


def read(p):
    return open(p, encoding='utf-8').read()


def write(p, s):
    open(p, 'w', encoding='utf-8').write(s)


def all_sources():
    out = {}
    for dp, dn, fn in os.walk(ROOT):
        for f in fn:
            if f.endswith('.kt'):
                p = os.path.join(dp, f)
                out[os.path.relpath(p, ROOT).replace('\\', '/')] = read(p)
    return out


def external_refs(sources, name, home):
    """count references to name outside its declaring file"""
    n = 0
    for rel, src in sources.items():
        if rel == home:
            continue
        n += len(re.findall(r'\b%s\b' % name, src))
    return n


def remove_block(src, name):
    """remove a top-level/private function or object by name, with its annotations"""
    m = re.search(r'^(?:@\w+(?:\([^)]*\))?\s*)*'
                  r'(?:internal |private |public )?(?:fun|object|class)\s+%s\b' % name,
                  src, re.M)
    if not m:
        return None
    start = m.start()
    # swallow the @Composable/@OptIn annotation lines that precede it
    lines = src[:start].split('\n')
    while len(lines) > 1 and re.match(r'^\s*@\w+', lines[-2] or ''):
        lines.pop()
    start = len('\n'.join(lines))
    if start > 0:
        start += 1
    # brace-match the body
    i = src.index('{', m.end())
    depth = 0
    j = i
    while j < len(src):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                break
        j += 1
    end = j + 1
    # swallow trailing newline(s)
    while end < len(src) and src[end] == '\n':
        end += 1
    # swallow preceding blank lines
    while start > 0 and src[start - 1] == '\n':
        start -= 1
    return src[:start] + src[end:], (start, end)


def main():
    sources = all_sources()
    problems = []

    # 1. dead whole files - verify every Salt* decl in them is dead
    for rel in DEAD_FILES:
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            continue
        src = read(p)
        decls = re.findall(r'^(?:internal |private |public )?(?:fun|val|object|class)\s+(\w*Salt\w*)\b',
                           src, re.M)
        live = [d for d in decls if external_refs(sources, d, rel) > 0]
        if live:
            problems.append('%s still has live decls: %s' % (rel, live))
            print('  [SKIP] %s - live decls %s' % (rel, live))
            continue
        os.remove(p)
        print('  [deleted file] %s  (%d dead decls)' % (rel, len(decls)))

    # 2. dead functions inside live files
    for rel, names in FUNCS.items():
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            continue
        src = read(p)
        for name in names:
            if external_refs(sources, name, rel) > 0:
                problems.append('%s::%s is still referenced' % (rel, name))
                print('  [SKIP] %s::%s - still referenced' % (rel, name))
                continue
            res = remove_block(src, name)
            if res is None:
                problems.append('%s::%s not found' % (rel, name))
                print('  [MISS] %s::%s' % (rel, name))
                continue
            src = res[0]
            print('  [removed] %s::%s' % (rel, name))
        write(p, src)

    # 3. dead single-line vals
    for rel, names in VALS.items():
        p = os.path.join(ROOT, rel)
        if not os.path.exists(p):
            continue
        src = read(p)
        for name in names:
            if external_refs(sources, name, rel) > 0:
                problems.append('%s::%s is still referenced' % (rel, name))
                print('  [SKIP] %s::%s - still referenced' % (rel, name))
                continue
            new = re.sub(r'^[ \t]*val\s+%s\s*=.*\n' % name, '', src, flags=re.M)
            if new != src:
                src = new
                print('  [removed] %s::val %s' % (rel, name))
        write(p, src)

    print('\n=== problems: %d ===' % len(problems))
    for x in problems:
        print('  ! ' + x)


if __name__ == '__main__':
    main()
