package com.ciclismo.portugal.di

import com.ciclismo.portugal.data.local.dao.NewsArticleDao
import com.ciclismo.portugal.data.remote.scraper.news.ABolaNewsScraper
import com.ciclismo.portugal.data.remote.scraper.news.BaseNewsScraper
import com.ciclismo.portugal.data.remote.scraper.news.JNNewsScraper
import com.ciclismo.portugal.data.remote.scraper.news.OJogoNewsScraper
import com.ciclismo.portugal.data.remote.scraper.news.RecordNewsScraper
import com.ciclismo.portugal.data.repository.NewsRepositoryImpl
import com.ciclismo.portugal.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NewsModule {

    @Binds
    @Singleton
    abstract fun bindNewsRepository(
        impl: NewsRepositoryImpl
    ): NewsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NewsScrapersModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideRecordNewsScraper(scraper: RecordNewsScraper): BaseNewsScraper = scraper

    @Provides
    @IntoSet
    @Singleton
    fun provideABolaNewsScraper(scraper: ABolaNewsScraper): BaseNewsScraper = scraper

    @Provides
    @IntoSet
    @Singleton
    fun provideOJogoNewsScraper(scraper: OJogoNewsScraper): BaseNewsScraper = scraper

    @Provides
    @IntoSet
    @Singleton
    fun provideJNNewsScraper(scraper: JNNewsScraper): BaseNewsScraper = scraper
}
