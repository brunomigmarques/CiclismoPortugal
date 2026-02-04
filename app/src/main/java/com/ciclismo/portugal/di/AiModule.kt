package com.ciclismo.portugal.di

import android.content.Context
import com.ciclismo.portugal.data.local.ai.AiActionExecutor
import com.ciclismo.portugal.data.local.ai.AiCapabilityChecker
import com.ciclismo.portugal.data.local.ai.AiContentGenerator
import com.ciclismo.portugal.data.local.ai.AiCoordinator
import com.ciclismo.portugal.data.local.ai.AiResponseParser
import com.ciclismo.portugal.data.local.ai.AiService
import com.ciclismo.portugal.data.local.ai.AiTriggerEngine
import com.ciclismo.portugal.data.local.ai.AiUsageTracker
import com.ciclismo.portugal.data.local.premium.PremiumManager
import com.ciclismo.portugal.data.remote.firebase.AuthService
import com.ciclismo.portugal.domain.repository.CyclistRepository
import com.ciclismo.portugal.domain.repository.FantasyTeamRepository
import com.ciclismo.portugal.domain.repository.RaceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    @Provides
    @Singleton
    fun provideAiCapabilityChecker(
        @ApplicationContext context: Context
    ): AiCapabilityChecker = AiCapabilityChecker(context)

    @Provides
    @Singleton
    fun provideAiUsageTracker(
        @ApplicationContext context: Context
    ): AiUsageTracker = AiUsageTracker(context)

    @Provides
    @Singleton
    fun provideAiResponseParser(): AiResponseParser = AiResponseParser()

    @Provides
    @Singleton
    fun provideAiService(
        capabilityChecker: AiCapabilityChecker,
        usageTracker: AiUsageTracker,
        responseParser: AiResponseParser
    ): AiService = AiService(capabilityChecker, usageTracker, responseParser)

    @Provides
    @Singleton
    fun provideAiContentGenerator(
        aiService: AiService
    ): AiContentGenerator = AiContentGenerator(aiService)

    @Provides
    @Singleton
    fun provideAiActionExecutor(
        fantasyTeamRepository: FantasyTeamRepository,
        cyclistRepository: CyclistRepository,
        raceRepository: RaceRepository
    ): AiActionExecutor = AiActionExecutor(fantasyTeamRepository, cyclistRepository, raceRepository)

    @Provides
    @Singleton
    fun provideAiTriggerEngine(
        @ApplicationContext context: Context
    ): AiTriggerEngine = AiTriggerEngine(context)

    @Provides
    @Singleton
    fun provideAiCoordinator(
        triggerEngine: AiTriggerEngine,
        authService: AuthService,
        fantasyTeamRepository: FantasyTeamRepository,
        raceRepository: RaceRepository,
        premiumManager: PremiumManager
    ): AiCoordinator = AiCoordinator(
        triggerEngine,
        authService,
        fantasyTeamRepository,
        raceRepository,
        premiumManager
    )
}
