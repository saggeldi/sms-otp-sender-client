package com.gonodono.smssender.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class SmsResultReceiver : BroadcastReceiver() {

    @Inject
    lateinit var scope: CoroutineScope

    @Inject
    lateinit var repository: SmsSenderRepository

    override fun onReceive(context: Context, intent: Intent) {
        val messageId = intent.data?.fragment?.toIntOrNull() ?: return

        val pendingResult = goAsync()
        scope.launch {
            when (intent.action) {
                ACTION_SMS_SENT -> {
                    repository.handleSendResult(
                        messageId,
                        pendingResult.resultCode,
                        intent.getBooleanExtra(EXTRA_IS_LAST_PART, false)
                    )
                }
                ACTION_SMS_DELIVERED -> {
                    val message = getResultMessageFromIntent(intent)
                    if (message != null) {
                        repository.handleDeliveryResult(
                            messageId,
                            message.status
                        )
                    }
                }
            }
            pendingResult.finish()
        }
    }
}

private fun getResultMessageFromIntent(intent: Intent): SmsMessage? =
    SmsMessage.createFromPdu(
        intent.getByteArrayExtra("pdu"),
        intent.getStringExtra("format")
    )