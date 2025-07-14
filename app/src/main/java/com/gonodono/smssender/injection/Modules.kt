package com.gonodono.smssender.injection

import android.content.Context
import androidx.room.Room
import com.gonodono.smssender.data.SmsSenderDatabase
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Singleton
    @Provides
    fun provideRepository(
        @ApplicationContext context: Context,
        scope: CoroutineScope,
        database: SmsSenderDatabase
    ) = SmsSenderRepository(context, scope, database)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context) =
        Room.databaseBuilder(
            context,
            SmsSenderDatabase::class.java,
            "sms_sender.db"
        ).build()
}

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @Singleton
    @Provides
    fun provideCoroutineScope() =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)
}