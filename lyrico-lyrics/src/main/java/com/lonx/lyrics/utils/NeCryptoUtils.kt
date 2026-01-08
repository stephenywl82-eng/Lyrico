package com.lonx.lyrics.utils

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NeCryptoUtils {
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private val DIGEST_TEXT = "nobody%suse%smd5forencrypt"

    fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun aesEncrypt(text: String, key: String): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(text.toByteArray())
    }

    fun aesDecrypt(data: ByteArray): String {
        return try {
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(EAPI_KEY.toByteArray(), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)
            String(cipher.doFinal(data))
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * EAPI 参数加密
     * @param url API 路径 (例如 /api/v3/song/detail)
     * @param jsonParams JSON 字符串参数
     */
    fun encryptParams(url: String, jsonParams: String): ByteArray {
        val message = String.format(DIGEST_TEXT, url, jsonParams)
        val digest = md5(message)
        val data = "$url-36cd479b6b5-$jsonParams-36cd479b6b5-$digest"
        return aesEncrypt(data, EAPI_KEY)
    }
}