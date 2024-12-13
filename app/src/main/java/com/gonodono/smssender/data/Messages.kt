package com.gonodono.smssender.data

import androidx.room.*
import com.gonodono.smssender.data.Message.DeliveryStatus
import com.gonodono.smssender.data.Message.SendStatus
import kotlinx.coroutines.flow.Flow
import java.util.*


@Entity(tableName = "messages")
class Message(
    val address: String,
    val body: String,
    @ColumnInfo(name = "send_status")
    val sendStatus: SendStatus? = null,
    @ColumnInfo(name = "delivery_status")
    val deliveryStatus: DeliveryStatus? = null,
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0
) {
    enum class SendStatus { Queued, Sent, Failed }

    enum class DeliveryStatus { Complete, Pending, Failed }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return id == (other as Message).id
    }

    override fun hashCode(): Int = id
}

@Dao
interface BaseMessageDao {
    @get:Query("SELECT * FROM messages ORDER BY id DESC")
    val allMessages: Flow<List<Message>>

    @get:Query(
        "SELECT * FROM messages WHERE send_status='Queued' " +
                "ORDER BY id ASC LIMIT 1"
    )
    val nextQueuedMessage: Flow<Message?>

    @Query("SELECT * FROM messages ORDER BY id DESC")
    suspend fun getAllMessages(): List<Message>

    @Insert
    suspend fun insertMessages(messages: List<Message>)

    @Query("UPDATE messages SET send_status=:status WHERE id=:id")
    suspend fun updateSendStatus(id: Int, status: SendStatus)

    @Query("UPDATE messages SET delivery_status=:status WHERE id=:id")
    suspend fun updateDeliveryStatus(id: Int, status: DeliveryStatus)

    @Query(
        "UPDATE messages SET send_status='Queued' WHERE " +
                "send_status='Failed' OR delivery_status='Failed'"
    )
    suspend fun resetFailedToQueued()
}