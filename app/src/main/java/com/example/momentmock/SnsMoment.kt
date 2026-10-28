package com.example.momentmock

import android.graphics.Bitmap

data class SnsComment(
    val avatar: Bitmap?,
    val name: String,
    val time: String,
    val text: String,

    val replyTo: String? = null,
)

data class SnsArticle(
    val title: String,

    val desc: String,
    val thumb: Bitmap?,

    val source: String = "",
)

data class SnsMoment(
    val avatar: Bitmap?,
    val nickname: String,
    val content: String,
    val images: List<Bitmap> = emptyList(),
    val video: Bitmap? = null,
    val timeText: String,
    val location: String? = null,

    val likes: List<Bitmap> = emptyList(),
    val comments: List<SnsComment> = emptyList(),

    val article: SnsArticle? = null,

    val isOwnFeed: Boolean = false,
    val isPrivate: Boolean = false,
)
