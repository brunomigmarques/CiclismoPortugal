package com.ciclismo.portugal.di

import android.content.Context
import androidx.room.Room
import com.ciclismo.portugal.data.local.CiclismoDatabase
import com.ciclismo.portugal.data.local.dao.ProvaDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CiclismoDatabase =
        Room.databaseBuilder(
            context,
            CiclismoDatabase::class.java,
            CiclismoDatabase.DATABASE_NAME
        )
            .addMigrations(
                CiclismoDatabase.MIGRATION_1_2,
                CiclismoDatabase.MIGRATION_2_3,
                CiclismoDatabase.MIGRATION_3_4,
                CiclismoDatabase.MIGRATION_4_5,
                CiclismoDatabase.MIGRATION_5_6
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    @Singleton
    fun provideProvaDao(database: CiclismoDatabase): ProvaDao =
        database.provaDao()

    @Provides
    @Singleton
    fun provideStravaGoalDao(database: CiclismoDatabase) =
        database.stravaGoalDao()

    @Provides
    @Singleton
    fun provideNewsArticleDao(database: CiclismoDatabase) =
        database.newsArticleDao()
}
