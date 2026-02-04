package com.ciclismo.portugal.di

import com.ciclismo.portugal.data.repository.FantasyTeamRepositoryImpl
import com.ciclismo.portugal.data.repository.LeagueRepositoryImpl
import com.ciclismo.portugal.data.repository.RaceRepositoryImpl
import com.ciclismo.portugal.data.repository.StageRepositoryImpl
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.LeagueRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import com.ciclismo.portugal.domain.repository.StageRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class FantasyModule {

    @Binds
    @Singleton
    abstract fun bindFantasyTeamRepository(
        fantasyTeamRepositoryImpl: FantasyTeamRepositoryImpl
    ): FantasyTeamRepository

    @Binds
    @Singleton
    abstract fun bindLeagueRepository(
        leagueRepositoryImpl: LeagueRepositoryImpl
    ): LeagueRepository

    @Binds
    @Singleton
    abstract fun bindRaceRepository(
        raceRepositoryImpl: RaceRepositoryImpl
    ): RaceRepository

    @Binds
    @Singleton
    abstract fun bindStageRepository(
        stageRepositoryImpl: StageRepositoryImpl
    ): StageRepository
}
