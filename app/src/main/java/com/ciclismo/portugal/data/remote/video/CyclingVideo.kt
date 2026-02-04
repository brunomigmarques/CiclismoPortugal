package com.ciclismo.portugal.data.remote.video

/**
 * Represents a cycling video from YouTube or other sources.
 * Used for displaying short-form video content in the app.
 */
data class CyclingVideo(
    val id: String,
    val title: String,
    val description: String = "",
    val thumbnailUrl: String,
    val videoUrl: String,
    val channelName: String,
    val durationSeconds: Int = 0,
    val source: VideoSource = VideoSource.YOUTUBE
) {
    val displayDuration: String
        get() {
            if (durationSeconds <= 0) return ""
            val minutes = durationSeconds / 60
            val seconds = durationSeconds % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }

    val isShort: Boolean
        get() = durationSeconds in 1..60
}

enum class VideoSource {
    YOUTUBE,
    INSTAGRAM,
    TIKTOK,
    OTHER
}
