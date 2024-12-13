package com.gonodono.smssender.repository

import android.content.Context
import android.provider.Telephony
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.gonodono.smssender.data.Message
import com.gonodono.smssender.data.Message.DeliveryStatus
import com.gonodono.smssender.data.Message.SendStatus
import com.gonodono.smssender.data.SendTask
import com.gonodono.smssender.data.SmsSenderDatabase
import com.gonodono.smssender.sms.getSmsManager
import com.gonodono.smssender.sms.sendMessage
import com.gonodono.smssender.work.SmsSendWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import kotlin.coroutines.resume

class SmsSenderRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    database: SmsSenderDatabase
) {
    private val messageDao = database.messageDao

    private val sendTaskDao = database.sendTaskDao

    val allMessages = messageDao.allMessages

    val latestSendTask = sendTaskDao.latestSendTask

    suspend fun getListOfMessage(): List<Message> {
        val messages = messageDao.getAllMessages()
        return messages
    }

    suspend fun insertMessagesAndSend(messages: List<Message>) {
        messageDao.insertMessages(messages)
        startImmediateSend()
    }

    suspend fun resetFailedAndRetry() {
        messageDao.resetFailedToQueued()
        startImmediateSend()
    }

    private fun startImmediateSend() {
        val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("SmsSend", ExistingWorkPolicy.KEEP, request)
    }

    private val errors = SmsErrors(context)

    suspend fun doSend(id: UUID): Boolean {
        val task = SendTask(id)
        sendTaskDao.insert(task)
        errors.resetForNextTask()

        suspendCancellableCoroutine { continuation ->
            scope.launch {
                val smsManager = getSmsManager(context)
                messageDao.nextQueuedMessage
                    .distinctUntilChanged()
                    .transformWhile { message ->
                        when {
                            errors.hadFatalSmsError -> {
                                task.state = SendTask.State.Failed
                                task.error = errors.createFatalMessage()
                                false
                            }
                            message == null -> {
                                task.state = SendTask.State.Succeeded
                                false
                            }
                            else -> {
                                emit(message)
                                true
                            }
                        }
                    }
                    .onEach { message ->
                        errors.resetForNextMessage()
                        sendMessage(context, smsManager, message)
                    }
                    .onCompletion { continuation.resume(Unit) }
                    .collect()
            }
        }

        sendTaskDao.update(task)
        return task.state == SendTask.State.Succeeded
    }

    suspend fun handleSendResult(
        messageId: Int,
        resultCode: Int,
        isLastPart: Boolean
    ) {
        errors.processResultCode(resultCode)
        if (isLastPart) {
            messageDao.updateSendStatus(
                messageId,
                if (errors.hadSmsError) SendStatus.Failed else SendStatus.Sent
            )
        }
    }

    suspend fun handleDeliveryResult(messageId: Int, smsStatus: Int) {
        val status = when {
            smsStatus == Telephony.Sms.STATUS_COMPLETE -> DeliveryStatus.Complete
            smsStatus >= Telephony.Sms.STATUS_FAILED -> DeliveryStatus.Failed
            else -> DeliveryStatus.Pending
        }
        messageDao.updateDeliveryStatus(messageId, status)
    }
}