# Lyrics Settings

Controls lyric search, format, and write behavior.

## Lyric Mode

Choose the preferred lyric format when searching:

- **Line-Timed Lyrics**: Traditional line-by-line LRC format.
- **Word-Timed Lyrics**: Lyrics with per-word timing.
- **Enhanced Word-Timed Lyrics**: Lyrics with per-word timing and additional metadata.
- **TTML**: TTML format (Timed Text Markup Language).

## Translation and Romanization

- **Romanization**: When enabled, requests romanized lyrics during search (useful for Japanese songs).
- **Translation**: When enabled, requests translated lyrics during search.
- **Download Translation Only**: When enabled, only downloads the translated portion if the song already has original lyrics.

## Lyric Line Order

**Lyric Line Order** decides the output order of **original / romanization / translation** in line-timed lyrics. The default is "Original / Romanization / Translation".

In the sheet, **drag** the enabled entries to reorder them; the change applies to lyrics written afterwards. Only enabled content appears in the sheet:

- With "Download Translation Only" enabled there is just the translation entry, so no reordering is needed.
- Romanization is absent unless romanization is enabled; translation is absent unless translation is enabled.

## Notes

Changing the lyric mode, romanization, or translation settings does **not auto-refresh** existing search results. Re-search to apply the new settings.

These settings also do not affect lyrics already saved in audio files.
