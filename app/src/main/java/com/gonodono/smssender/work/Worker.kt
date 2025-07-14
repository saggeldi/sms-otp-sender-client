package com.gonodono.smssender.work

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.gonodono.smssender.R
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject


@HiltWorker
internal class SmsSendWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: SmsSenderRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork() =
        if (repository.doSend(id)) Result.success() else Result.failure()

    override suspend fun getForegroundInfo() =
        ForegroundInfo(420, createNotification(applicationContext))
}

private const val SENDER_CHANNEL_ID = "sms_send_worker"

private const val SENDER_CHANNEL_NAME = "SMS Send Worker"

private fun createNotification(context: Context): Notification {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val manager = context.getSystemService(NotificationManager::class.java)
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            manager.getNotificationChannel(SENDER_CHANNEL_ID) == null
        ) {
            manager.createNotificationChannel(
                NotificationChannel(
                    SENDER_CHANNEL_ID,
                    SENDER_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }
        return NotificationCompat.Builder(context, SENDER_CHANNEL_ID)
            .setContentTitle(SENDER_CHANNEL_NAME)
            .setContentText("Sending…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .build()
    } else {
        return NotificationCompat.Builder(context)
            .setContentTitle(SENDER_CHANNEL_NAME)
            .setContentText("Sending…")
            .build()
    }

}