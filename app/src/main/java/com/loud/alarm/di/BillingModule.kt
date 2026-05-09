package com.loud.alarm.di

import android.content.Context
import com.loud.alarm.analytics.AnalyticsLogger
import com.loud.alarm.billing.BillingManager
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
        analyticsLogger: AnalyticsLogger
    ): BillingManager {
        return BillingManager(context, analyticsLogger)
    }
}
