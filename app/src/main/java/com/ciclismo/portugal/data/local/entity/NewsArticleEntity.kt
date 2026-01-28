package com.ciclismo.portugal.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.ciclismo.portugal.domain.model.NewsArticle

@Entity(
    tableName = "news_articles",
    indices = [Index(value = ["contentHash"], unique = true)]
)
data class NewsArticleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val summary: String,
    val url: String,
    val imageUrl: String?,
    val source: String,
    val publishedAt: Long,
    val author: String? = null,
    val contentHash: String,
    val createdAt: Long = System.currentTimeMillis()
)

fun NewsArticleEntity.toDomain(): NewsArticle = NewsArticle(
    id = id,
    title = title,
    summary = summary,
    url = url,
    imageUrl = imageUrl,
    source = source,
    publishedAt = publishedAt,
    author = author,
    contentHash = contentHash
)

fun NewsArticle.toEntity(): NewsArticleEntity = NewsArticleEntity(
    id = id,
    title = title,
    summary = summary,
    url = url,
    imageUrl = imageUrl,
    source = source,
    publishedAt = publishedAt,
    author = author,
    contentHash = contentHash
)
