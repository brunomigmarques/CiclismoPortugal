package com.ciclismo.portugal.di

import com.ciclismo.portugal.data.remote.cycling.CyclingDataSource
import com.ciclismo.portugal.data.remote.cycling.ProCyclingStatsScraper
import com.ciclismo.portugal.data.repository.CyclistRepositoryImpl
import com.ciclismo.portugal.domain.repository.CyclistRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CyclingModule {

    @Binds
    @Singleton
    abstract fun bindCyclingDataSource(
        proCyclingStatsScraper: ProCyclingStatsScraper
    ): CyclingDataSource

    @Binds
    @Singleton
    abstract fun bindCyclistRepository(
        cyclistRepositoryImpl: CyclistRepositoryImpl
    ): CyclistRepository
}
