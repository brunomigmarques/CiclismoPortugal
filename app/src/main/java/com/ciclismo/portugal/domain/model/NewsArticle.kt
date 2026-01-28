package com.ciclismo.portugal.domain.model

data class NewsArticle(
    val id: Long = 0,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val source: String,
    val publishedAt: Long,
    val author: String? = null,
    val contentHash: String // Para detectar duplicados
)
