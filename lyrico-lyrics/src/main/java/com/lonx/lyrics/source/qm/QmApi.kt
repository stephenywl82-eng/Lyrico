package com.lonx.lyrics.source.qm


import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.POST

// 请求体结构
@Serializable
data class QmRequestBody(
    val comm: Map<String, String>,
    val req_0: QmRequestModule // 简化为单请求
)

@Serializable
data class QmRequestModule(
    val method: String,
    val module: String,
    val param: Map<String, JsonElement> // 支持数字和字符串值
)

interface QmApi {
    @POST("cgi-bin/musicu.fcg")
    suspend fun request(@Body body: QmRequestBody): JsonObject // 返回完整 JSON 自行解析
}