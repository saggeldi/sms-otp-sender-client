package com.gonodono.smssender.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telephony.SmsManager
import com.gonodono.smssender.BuildConfig
import com.gonodono.smssender.data.Message
import java.util.*


internal const val ACTION_SMS_SENT =
    "${BuildConfig.APPLICATION_ID}.action.SMS_SENT"

internal const val ACTION_SMS_DELIVERED =
    "${BuildConfig.APPLICATION_ID}.action.SMS_DELIVERED"

internal const val EXTRA_IS_LAST_PART =
    "${BuildConfig.APPLICATION_ID}.extra.IS_LAST_PART"

internal fun getSmsManager(context: Context): SmsManager = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        context.getSystemService(SmsManager::class.java)
    }
    else -> {
        @Suppress("DEPRECATION")
        SmsManager.getDefault()
    }
}

internal fun sendMessage(
    context: Context,
    manager: SmsManager,
    message: Message
) {
    val parts = manager.divideMessage(message.body)
    val partCount = parts.size
    val sendIntents = arrayListOf<PendingIntent>()
    val deliveryIntents = arrayListOf<PendingIntent?>()

    for (partNumber in 1..partCount) {
        val isLastPart = partNumber == partCount
        sendIntents += PendingIntent.getBroadcast(
            context,
            partNumber,
            createSendIntent(message.id, context, isLastPart),
            RESULT_FLAGS
        )
        deliveryIntents += if (isLastPart) {
            PendingIntent.getBroadcast(
                context,
                0,
                createDeliveryIntent(context, message.id),
                RESULT_FLAGS
            )
        } else null
    }

    manager.sendMultipartTextMessage(
        message.address,
        null,
        parts,
        sendIntents,
        deliveryIntents
    )
}

private fun createSendIntent(
    messageId: Int,
    context: Context,
    isLastPart: Boolean
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_SENT)
    .putExtra(EXTRA_IS_LAST_PART, isLastPart)

// internal for use in fake delivery reporting
internal fun createDeliveryIntent(
    context: Context,
    messageId: Int
) = createResultIntent(context, messageId)
    .setAction(ACTION_SMS_DELIVERED)

private fun createResultIntent(
    context: Context,
    messageId: Int
) = Intent(
    null,
    Uri.fromParts("app", BuildConfig.APPLICATION_ID, messageId.toString()),
    context,
    SmsResultReceiver::class.java
)

private val RESULT_FLAGS =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_ONE_SHOT
    }