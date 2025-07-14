package com.gonodono.smssender.data

import androidx.room.*
import com.gonodono.smssender.work.*
import kotlinx.coroutines.flow.Flow
import java.util.*


@Entity(tableName = "send_tasks")
class SendTask(
    @PrimaryKey
    val id: UUID,
    var state: State = State.Running,
    var error: String? = null
) {
    enum class State { Running, Succeeded, Failed }
}

@Dao
interface SendTaskDao {
    @get:Query("SELECT * FROM send_tasks ORDER BY rowid DESC LIMIT 1")
    val latestSendTask: Flow<SendTask?>

    @Insert
    suspend fun insert(task: SendTask)

    @Update
    suspend fun update(task: SendTask)
}