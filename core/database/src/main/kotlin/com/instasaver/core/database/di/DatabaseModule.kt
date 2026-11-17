package com.instasaver.core.database.di

import android.content.Context
import androidx.room.Room
import com.instasaver.core.database.InstaSaverDatabase
import com.instasaver.core.database.dao.DownloadHistoryDao
import com.instasaver.core.database.dao.MediaCacheDao
import com.instasaver.core.database.dao.ProfileCacheDao
import com.instasaver.core.database.dao.WatchlistDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): InstaSaverDatabase {
        return Room.databaseBuilder(
            context,
            InstaSaverDatabase::class.java,
            "instasaver.db"
        )
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideWatchlistDao(database: InstaSaverDatabase): WatchlistDao {
        return database.watchlistDao()
    }

    @Provides
    fun provideMediaCacheDao(database: InstaSaverDatabase): MediaCacheDao {
        return database.mediaCacheDao()
    }

    @Provides
    fun provideProfileCacheDao(database: InstaSaverDatabase): ProfileCacheDao {
        return database.profileCacheDao()
    }

    @Provides
    fun provideDownloadHistoryDao(database: InstaSaverDatabase): DownloadHistoryDao {
        return database.downloadHistoryDao()
    }

    @Provides
    @Singleton
    fun provideDatabaseRepository(
        watchlistDao: WatchlistDao,
        mediaCacheDao: MediaCacheDao,
        profileCacheDao: ProfileCacheDao,
        downloadHistoryDao: DownloadHistoryDao
    ): com.instasaver.core.database.DatabaseRepository {
        return com.instasaver.core.database.DatabaseRepositoryImpl(
            watchlistDao = watchlistDao,
            mediaCacheDao = mediaCacheDao,
            profileCacheDao = profileCacheDao,
            downloadHistoryDao = downloadHistoryDao
        )
    }
}
