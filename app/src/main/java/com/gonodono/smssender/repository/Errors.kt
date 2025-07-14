package com.gonodono.smssender.repository

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.telephony.SmsManager
import java.util.*


internal class SmsErrors(context: Context) {

    // These errors are persisted for the slim chance that the app is killed in
    // the time between a send and the receipt of its results. Tying them to the
    // task instance would've made retrieval unwieldy for that case, and we
    // don't need the specific errors in messages, so this seemed appropriate.

    private val preferences =
        context.getSharedPreferences("sms_errors.xml", Context.MODE_PRIVATE)

    private var fatalSmsError: Int
        get() = preferences.getInt(PREF_FATAL_SMS_ERROR, SMS_ERROR_NONE)
        set(value) {
            preferences.edit().putInt(PREF_FATAL_SMS_ERROR, value).apply()
        }

    private var lastSmsError: Int
        get() = preferences.getInt(PREF_LAST_SMS_ERROR, SMS_ERROR_NONE)
        set(value) {
            preferences.edit().putInt(PREF_LAST_SMS_ERROR, value).apply()
        }

    fun resetForNextTask() {
        fatalSmsError = SMS_ERROR_NONE
    }

    fun resetForNextMessage() {
        lastSmsError = SMS_ERROR_NONE
    }

    fun processResultCode(resultCode: Int) {
        if (resultCode != Activity.RESULT_OK) {
            lastSmsError = resultCode
            if (resultCode in FatalSmsErrors) {
                fatalSmsError = resultCode
            }
        }
    }

    val hadSmsError: Boolean get() = lastSmsError != SMS_ERROR_NONE

    val hadFatalSmsError: Boolean get() = fatalSmsError != SMS_ERROR_NONE

    fun createFatalMessage() = "SMS Error $fatalSmsError"
}

private const val PREF_FATAL_SMS_ERROR = "fatal_sms_error"

private const val PREF_LAST_SMS_ERROR = "last_sms_error"

private const val SMS_ERROR_NONE = 0

// No issue if newer error ints checked on older versions
@SuppressLint("InlinedApi")
private val FatalSmsErrors = intArrayOf(
    // You may wish to adjust this selection, depending on your specific
    // setup (or just to double-check that I got all that I should've).
    // The full list is available in the docs for SmsManager.
    // https://developer.android.com/reference/android/telephony/SmsManager#sendMultipartTextMessage(java.lang.String,%20java.lang.String,%20java.util.ArrayList%3Cjava.lang.String%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E,%20java.util.ArrayList%3Candroid.app.PendingIntent%3E)
    SmsManager.RESULT_ERROR_GENERIC_FAILURE,
    SmsManager.RESULT_ERROR_LIMIT_EXCEEDED,
    SmsManager.RESULT_ERROR_NO_SERVICE,
    SmsManager.RESULT_ERROR_RADIO_OFF,
    SmsManager.RESULT_INTERNAL_ERROR,
    SmsManager.RESULT_INVALID_SMSC_ADDRESS,
    SmsManager.RESULT_MODEM_ERROR,
    SmsManager.RESULT_NO_MEMORY,
    SmsManager.RESULT_NO_RESOURCES,
    SmsManager.RESULT_RADIO_NOT_AVAILABLE,
    SmsManager.RESULT_SMS_BLOCKED_DURING_EMERGENCY,
    SmsManager.RESULT_SYSTEM_ERROR,
    SmsManager.RESULT_UNEXPECTED_EVENT_STOP_SENDING,
    SmsManager.RESULT_RIL_ACCESS_BARRED,
    SmsManager.RESULT_RIL_BLOCKED_DUE_TO_CALL,
    SmsManager.RESULT_RIL_INTERNAL_ERR,
    SmsManager.RESULT_RIL_INVALID_MODEM_STATE,
    SmsManager.RESULT_RIL_INVALID_SMSC_ADDRESS,
    SmsManager.RESULT_RIL_INVALID_STATE,
    SmsManager.RESULT_RIL_MODEM_ERR,
    SmsManager.RESULT_RIL_NETWORK_NOT_READY,
    SmsManager.RESULT_RIL_NO_MEMORY,
    SmsManager.RESULT_RIL_NO_RESOURCES,
    SmsManager.RESULT_RIL_OPERATION_NOT_ALLOWED,
    SmsManager.RESULT_RIL_RADIO_NOT_AVAILABLE,
    SmsManager.RESULT_RIL_REQUEST_RATE_LIMITED,
    SmsManager.RESULT_RIL_SIMULTANEOUS_SMS_AND_CALL_NOT_ALLOWED,
    SmsManager.RESULT_RIL_SIM_ABSENT,
    SmsManager.RESULT_RIL_SYSTEM_ERR
)