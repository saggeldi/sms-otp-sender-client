package com.gonodono.smssender.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface MessageDao : BaseMessageDao {

    @Query(
        "SELECT * FROM messages WHERE address=:address AND " +
                "body=:body AND delivery_status IS NULL LIMIT 1"
    )
    suspend fun checkForFakeDeliveryReport(
        address: String,
        body: String
    ): Message?
}