package com.instasaver.core.extractor.di

import com.instasaver.core.extractor.ExtractorRepository
import com.instasaver.core.extractor.ExtractorRepositoryImpl
import com.instasaver.core.extractor.ProfileRepository
import com.instasaver.core.extractor.ProfileRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractorModule {

    @Binds
    @Singleton
    abstract fun bindExtractorRepository(
        impl: ExtractorRepositoryImpl
    ): ExtractorRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
}
