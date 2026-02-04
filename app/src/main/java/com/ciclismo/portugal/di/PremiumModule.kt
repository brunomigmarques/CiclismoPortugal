package com.ciclismo.portugal.di

import android.content.Context
import com.ciclismo.portugal.data.local.premium.PremiumManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PremiumModule {

    @Provides
    @Singleton
    fun providePremiumManager(
        @ApplicationContext context: Context
    ): PremiumManager = PremiumManager(context)
}
