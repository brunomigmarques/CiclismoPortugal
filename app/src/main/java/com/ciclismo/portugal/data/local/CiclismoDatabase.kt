package com.ciclismo.portugal.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ciclismo.portugal.data.local.dao.NewsArticleDao
import com.ciclismo.portugal.data.local.dao.ProvaDao
import com.ciclismo.portugal.data.local.dao.StravaGoalDao
import com.ciclismo.portugal.data.local.entity.NewsArticleEntity
import com.ciclismo.portugal.data.local.entity.ProvaEntity
import com.ciclismo.portugal.data.local.entity.StravaGoalEntity

@Database(
    entities = [ProvaEntity::class, StravaGoalEntity::class, NewsArticleEntity::class],
    version = 6,
    exportSchema = false
)
abstract class CiclismoDatabase : RoomDatabase() {
    abstract fun provaDao(): ProvaDao
    abstract fun stravaGoalDao(): StravaGoalDao
    abstract fun newsArticleDao(): NewsArticleDao

    companion object {
        const val DATABASE_NAME = "ciclismo_portugal_db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE provas ADD COLUMN hidden INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create unique index to prevent duplicate events
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_provas_nome_data_source ON provas(nome, data, source)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create strava_goals table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS strava_goals (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        targetValue REAL NOT NULL,
                        currentValue REAL NOT NULL,
                        year INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create news_articles table
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS news_articles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        url TEXT NOT NULL,
                        imageUrl TEXT,
                        source TEXT NOT NULL,
                        publishedAt INTEGER NOT NULL,
                        author TEXT,
                        contentHash TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_news_articles_contentHash ON news_articles(contentHash)")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add imageUrl column to provas table
                database.execSQL("ALTER TABLE provas ADD COLUMN imageUrl TEXT")
            }
        }
    }
}
