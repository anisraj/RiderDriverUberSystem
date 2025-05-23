package me.anisjamadar.ridebookinguberpartnerapp.di.module

import android.util.Log
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.anisjamadar.locationsharingapplication.BuildConfig
import me.anisjamadar.ridebookinguberpartnerapp.data.api.NetworkService
import me.anisjamadar.ridebookinguberpartnerapp.di.BaseClient
import me.anisjamadar.ridebookinguberpartnerapp.di.BaseUrl
import me.anisjamadar.ridebookinguberpartnerapp.di.LoggingInterceptor
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ApplicationModule {

    @BaseUrl
    @Provides
    fun provideBaseUrl(): String = "https://api.olamaps.io"

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory = GsonConverterFactory.create()

    @BaseClient
    @Provides
    @Singleton
    fun provideBaseClient(
        @LoggingInterceptor loggingInterceptor: Lazy<Interceptor>
    ): OkHttpClient {
        val builder = OkHttpClient.Builder().apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(loggingInterceptor.get())
            }
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @LoggingInterceptor
    fun provideLoggingInterceptor(): Interceptor {
        val logger = HttpLoggingInterceptor { m -> Log.d("okhttp", m) }
        logger.level = HttpLoggingInterceptor.Level.BODY
        return logger
    }

    @Provides
    @Singleton
    fun provideNetworkService(
        @BaseUrl baseUrl: String,
        gsonConverterFactory: GsonConverterFactory,
        @BaseClient okHttpClient: OkHttpClient
    ): NetworkService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(gsonConverterFactory)
            .build()
            .create(NetworkService::class.java)
    }
}