package com.lonx.lyrico.data.model

import com.lonx.lyrico.data.dto.ReleaseInfo
import java.io.IOException

sealed class UpdateCheckResult {
    data class NewVersion(val info: ReleaseInfo) : UpdateCheckResult()

    object NoUpdateAvailable : UpdateCheckResult()

    data class NetworkError(val exception: IOException) : UpdateCheckResult()

    object TimeoutError : UpdateCheckResult()

    data class ApiError(val code: Int, val message: String) : UpdateCheckResult()

    /** 失败：无法解析返回的 JSON 数据 */
    object ParsingError : UpdateCheckResult()
}