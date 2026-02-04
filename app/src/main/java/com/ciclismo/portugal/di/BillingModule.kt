package com.ciclismo.portugal.di

import android.content.Context
import com.ciclismo.portugal.data.billing.BillingManager
import com.ciclismo.portugal.data.local.premium.PremiumManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {

    @Provides
    @Singleton
    fun provideBillingManager(
        @ApplicationContext context: Context,
        premiumManager: PremiumManager
    ): BillingManager = BillingManager(context, premiumManager)
}
