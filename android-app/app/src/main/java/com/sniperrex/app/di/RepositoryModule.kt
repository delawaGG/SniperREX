package com.sniperrex.app.di

import com.sniperrex.app.data.repository.LocalSignalRepository
import com.sniperrex.app.domain.repository.SignalRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSignalRepository(impl: LocalSignalRepository): SignalRepository
}
