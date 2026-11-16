package com.instasaver.core.network.di

import android.content.Context
import com.instasaver.core.network.InstagramCookieJar
import com.instasaver.core.network.InstagramHeadersInterceptor
import com.instasaver.core.network.InstagramSessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CookieJar
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideInstagramCookieJar(): InstagramCookieJar {
        return InstagramCookieJar()
    }

    @Provides
    @Singleton
    fun provideCookieJar(cookieJar: InstagramCookieJar): CookieJar {
        return cookieJar
    }

    @Provides
    @Singleton
    fun provideHeadersInterceptor(): InstagramHeadersInterceptor {
        return InstagramHeadersInterceptor()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        cookieJar: InstagramCookieJar,
        headersInterceptor: InstagramHeadersInterceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .cookieJar(cookieJar)
            .addInterceptor(headersInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideSessionManager(
        @ApplicationContext context: Context,
        cookieJar: InstagramCookieJar
    ): InstagramSessionManager {
        return InstagramSessionManager(context, cookieJar)
    }
}
