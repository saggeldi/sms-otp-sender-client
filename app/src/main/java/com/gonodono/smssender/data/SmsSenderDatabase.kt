package com.gonodono.smssender.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Message::class, SendTask::class], version = 1)
abstract class SmsSenderDatabase : RoomDatabase() {

    abstract val messageDao: MessageDao

    abstract val sendTaskDao: SendTaskDao
}