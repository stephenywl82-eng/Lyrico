package com.lonx.audiotag.rw

/**
 * 把 native 层返回的诊断信息翻译成用户可理解、可操作的提示。
 *
 * 说明：audiotag 模块无法访问 App 的 R 字符串资源，且本 App 以中文为主要语言，
 * 这里用常量直接给出中文文案。新增特例时，只需在 native 设置对应的机器可读标记
 * （如 "KUWO_DRM_FILE"），再在此处追加一份友好文案即可。
 */
internal object TagErrorMessages {
    private const val KUWO_DRM_MARKER = "KUWO_DRM_FILE"
    const val KUWO_DRM_MESSAGE =
        "无法编辑：该文件是酷我音乐加密文件（DRM 保护，文件头标识 yeelion-kuwo-tme），" +
        "并非标准音频格式，无法写入标签、封面或歌词。" +
        "请先在酷我音乐中将其导出 / 转换为标准格式（如 FLAC、MP3）后再编辑。"

    /** 若 [detail] 命中已知特例，返回对应友好提示；否则返回 null（由调用方附带原始诊断）。 */
    fun friendly(detail: String?): String? {
        if (detail == null) return null
        // 标记可能出现在字符串任意位置（native 会在前面拼 "[savePropertyMap] " 等前缀），故用 contains。
        if (detail.contains(KUWO_DRM_MARKER)) return KUWO_DRM_MESSAGE
        return null
    }
}
