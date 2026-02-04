package com.ciclismo.portugal.di

import android.content.SharedPreferences
import com.ciclismo.portugal.data.remote.strava.StravaApiService
import com.ciclismo.portugal.data.remote.strava.StravaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StravaModule {

    private const val STRAVA_BASE_URL = "https://www.strava.com/"

    @Provides
    @Singleton
    @Named("StravaOkHttpClient")
    fun provideStravaOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("StravaRetrofit")
    fun provideStravaRetrofit(@Named("StravaOkHttpClient") okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(STRAVA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideStravaApiService(@Named("StravaRetrofit") retrofit: Retrofit): StravaApiService {
        return retrofit.create(StravaApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStravaRepository(
        stravaApiService: StravaApiService,
        sharedPreferences: SharedPreferences
    ): StravaRepository {
        return StravaRepository(stravaApiService, sharedPreferences)
    }
}
