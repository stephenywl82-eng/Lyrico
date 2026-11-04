package com.lonx.lyrico.data.player

import com.lonx.lyrico.App
import com.lonx.lyrico.R

object FriendlyPlayerRegistry {
    // 仅保留 Moto Music 跳转链接（原 Cone Player / Halcyon / Salt Player 等外部播放器链接已移除）
    val players: List<FriendlyPlayer> = listOf(
        FriendlyPlayer(
            id = "moto_music",
            displayNameRes = R.string.friendly_player_moto_music,
            packageNames = listOf("com.sdw.music.player.cn"),
            websiteUrl = App.MOTO_MUSIC_GITHUB_URL
        )
    )
}
