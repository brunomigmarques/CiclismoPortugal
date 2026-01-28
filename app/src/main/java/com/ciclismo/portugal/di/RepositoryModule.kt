package com.ciclismo.portugal.di

import com.ciclismo.portugal.data.remote.scraper.BaseScraper
import com.ciclismo.portugal.data.remote.scraper.BikeServiceScraper
import com.ciclismo.portugal.data.remote.scraper.CabreiraScraper
import com.ciclismo.portugal.data.repository.ProvaRepositoryImpl
import com.ciclismo.portugal.domain.repository.ProvaRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProvaRepository(impl: ProvaRepositoryImpl): ProvaRepository
}

@Module
@InstallIn(SingletonComponent::class)
object ScraperModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideCabreiraScraper(cabreiraScraper: CabreiraScraper): BaseScraper = cabreiraScraper

    @Provides
    @IntoSet
    @Singleton
    fun provideBikeServiceScraper(bikeServiceScraper: BikeServiceScraper): BaseScraper = bikeServiceScraper
}
