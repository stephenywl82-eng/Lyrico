package com.lonx.lyrics.utils

import com.lonx.lyrics.model.LyricsResult
import com.lonx.lyrics.model.LyricsData
import com.lonx.lyrics.model.LyricsLine
import com.lonx.lyrics.model.LyricsWord
import java.util.regex.Pattern

object QrcParser {

    private val QRC_XML_PATTERN = Pattern.compile("<Lyric_1 LyricType=\"1\" LyricContent=\"(.*?)\"/>", Pattern.DOTALL)
    private val QRC_LINE_PATTERN = Pattern.compile("^\\[(\\d+),(\\d+)](.*)$")
    private val WORD_PATTERN = Pattern.compile("(?:^\\[\\d+,\\d+])?((?:(?!\\(\\d+,\\d+\\)).)*)\\((\\d+),(\\d+)\\)")
    private val TAG_PATTERN = Pattern.compile("^\\[(\\w+):([^]]*)]$")
    private val LRC_PATTERN = Pattern.compile("^\\[(\\d+):(\\d+\\.\\d+)](.*)$")

    fun parse(lyricsData: LyricsData): LyricsResult {
        val tags = mutableMapOf<String, String>()
        val qrcText = lyricsData.original ?: ""

        qrcText.lines().forEach { rawLine ->
            val tagMatcher = TAG_PATTERN.matcher(rawLine.trim())
            if (tagMatcher.matches()) {
                tags[tagMatcher.group(1)!!] = tagMatcher.group(2) ?: ""
            }
        }

        val origList = parseQrcFormat(qrcText)

        val rawTransList = if (!lyricsData.translated.isNullOrBlank()) parseLrcFormat(lyricsData.translated) else null

        val rawRomaList = if (!lyricsData.romanization.isNullOrBlank()) parseQrcFormat(lyricsData.romanization) else null

        val alignedTransList = lyricsMerge(origList, rawTransList)
        val alignedRomaList = lyricsMerge(origList, rawRomaList)

        return LyricsResult(tags, origList, alignedTransList, alignedRomaList)
    }

    /**
     * 通用 QRC 格式解析（逐字），原文和罗马音共用此逻辑
     */
    private fun parseQrcFormat(text: String): List<LyricsLine> {
        if (text.isBlank()) return emptyList()

        var content = text
        val xmlMatcher = QRC_XML_PATTERN.matcher(text)
        if (xmlMatcher.find()) {
            content = xmlMatcher.group(1) ?: ""
        }

        val resultList = ArrayList<LyricsLine>()
        val lines = content.lines()

        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue

            val tagMatcher = TAG_PATTERN.matcher(line)
            if (tagMatcher.matches()) continue

            val lineMatcher = QRC_LINE_PATTERN.matcher(line)
            if (lineMatcher.matches()) {
                val lineStart = lineMatcher.group(1)!!.toLong()
                val lineDuration = lineMatcher.group(2)!!.toLong()
                val lineEnd = lineStart + lineDuration
                val lineContent = lineMatcher.group(3) ?: ""

                val words = ArrayList<LyricsWord>()
                val wordMatcher = WORD_PATTERN.matcher(lineContent)
                val wordList = mutableListOf<Pair<Long, String>>() // Pair<start, text>

                while (wordMatcher.find()) {
                    val wordText = wordMatcher.group(1) ?: ""
                    val wordStart = wordMatcher.group(2)!!.toLong()
                    wordList.add(wordStart to wordText)
                }

                for (i in wordList.indices) {
                    val (wordStart, wordText) = wordList[i]
                    val wordEnd = if (i < wordList.size - 1) {
                        wordList[i + 1].first
                    } else {
                        lineEnd
                    }
                    words.add(LyricsWord(start = wordStart, end = wordEnd, text = wordText))
                }

                if (words.isEmpty()) {
                    words.add(LyricsWord(lineStart, lineEnd, lineContent))
                }
                resultList.add(LyricsLine(lineStart, lineEnd, words))
            }
        }
        return resultList
    }

    /**
     * 解析 LRC 格式 (通常用于翻译)
     */
    private fun parseLrcFormat(text: String): List<LyricsLine>? {
        if (text.isBlank()) return null
        data class TempLine(val start: Long, val content: String)
        val tempLines = ArrayList<TempLine>()

        val lines = text.lines()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val matcher = LRC_PATTERN.matcher(line)
            if (matcher.matches()) {
                val minutes = matcher.group(1)!!.toLong()
                val secondsStr = matcher.group(2)!!
                val totalMillis = (minutes * 60 * 1000) + (secondsStr.toDouble() * 1000).toLong()
                val content = matcher.group(3) ?: ""
                tempLines.add(TempLine(totalMillis, content))
            }
        }
        if (tempLines.isEmpty()) return null
        tempLines.sortBy { it.start }

        val resultList = ArrayList<LyricsLine>()
        for (i in tempLines.indices) {
            val current = tempLines[i]
            val next = if (i < tempLines.size - 1) tempLines[i + 1] else null
            val endTime = if (next != null) maxOf(current.start, next.start - 10) else current.start + 2000

            val words = listOf(LyricsWord(current.start, endTime, current.content))
            resultList.add(LyricsLine(current.start, endTime, words))
        }
        return resultList
    }

    /**
     * 遍历原文的每一行，在附属列表（翻译/罗马音）中寻找时间最接近的一行。
     */
    private fun lyricsMerge(
        originalLines: List<LyricsLine>,
        transLines: List<LyricsLine>?
    ): List<LyricsLine>? {
        if (transLines.isNullOrEmpty()) return null

        val sortedTransLines = transLines.sortedBy { it.start }
        val alignedList = ArrayList<LyricsLine>()

        var transIdx = 0
        val transCount = sortedTransLines.size

        for (i in originalLines.indices) {
            val orig = originalLines[i]
            val winStart = orig.start
            val winEnd = if (i < originalLines.size - 1) {
                originalLines[i + 1].start
            } else {
                Long.MAX_VALUE
            }

            var matchedLine: LyricsLine? = null

            while (transIdx < transCount) {
                val trans = sortedTransLines[transIdx]

                if (trans.start < winStart - 500) {
                    transIdx++
                    continue
                }

                if (trans.start >= winEnd) {
                    break
                }

                // 命中目标
                matchedLine = trans
                transIdx++
                break
            }

            if (matchedLine != null) {
                alignedList.add(LyricsLine(orig.start, orig.end, matchedLine.words))
            } else {
                val emptyWords = listOf(LyricsWord(orig.start, orig.end, ""))
                alignedList.add(LyricsLine(orig.start, orig.end, emptyWords))
            }
        }

        return alignedList
    }
}