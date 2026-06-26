package com.tc128.giamdinhnative.di

import android.content.Context
import androidx.room.Room
import com.tc128.giamdinhnative.data.local.AppDatabase
import com.tc128.giamdinhnative.data.local.LookupDao
import com.tc128.giamdinhnative.data.local.PhotoDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "giamdinhnative.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun providePhotoDao(db: AppDatabase): PhotoDao = db.photoDao()

    @Provides
    fun provideLookupDao(db: AppDatabase): LookupDao = db.lookupDao()
}
