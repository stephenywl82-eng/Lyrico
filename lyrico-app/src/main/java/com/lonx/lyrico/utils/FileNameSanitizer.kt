package com.lonx.lyrico.utils

object FileNameSanitizer {
    private val INVALID_CHARS = Regex("[\\\\/:*?\"<>|]")

    fun sanitize(fileName: String): String {
        return fileName.replace(INVALID_CHARS, "_").trim('_', ' ')
    }

    fun sanitizePath(path: String): String {
        val parts = path.split('/')
        return parts.joinToString("/") { sanitize(it) }
    }
}
