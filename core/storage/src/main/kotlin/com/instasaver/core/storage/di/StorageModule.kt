package com.instasaver.core.storage.di

import android.content.Context
import com.instasaver.core.storage.AudioDemuxer
import com.instasaver.core.storage.MediaStorageService
import com.instasaver.core.storage.MediaStorageServiceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    fun provideAudioDemuxer(): AudioDemuxer {
        return AudioDemuxer()
    }

    @Provides
    @Singleton
    fun provideMediaStorageService(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient,
        audioDemuxer: AudioDemuxer
    ): MediaStorageService {
        return MediaStorageServiceImpl(context, okHttpClient, audioDemuxer)
    }
}
