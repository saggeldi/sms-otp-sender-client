package com.gonodono.smssender

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.room.Room
import com.gonodono.smssender.data.SmsSenderDatabase
import com.gonodono.smssender.injection.SmsSenderApplication
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.AndroidEntryPoint
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.json.JSONObject
import java.net.URI
import javax.inject.Inject

class SmsForegroundService : Service() {

    private val defaultHost = "http://172.16.18.115:6415"
//    private val defaultHost = "http://216.250.13.152:3010"
    private val eventName = "verification-phone"
//    private val eventName = "otp"
    private var mSocket: Socket? = null
    private val context: Context = SmsSenderApplication.INSTANCE
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val db = Room.databaseBuilder(
        context,
        SmsSenderDatabase::class.java,
        "sms_sender.db"
    ).allowMainThreadQueries().build()
    val repo = SmsSenderRepository(context, scope, db)
    val viewModel: MainViewModel = MainViewModel(repo)

    override fun onCreate() {
        super.onCreate()
        Log.d("SmsForegroundService", "Socket Connecting!")
        createNotificationChannel()
        startForeground(1, createNotification())
        createSocket()
        connectWebSocket()
        viewModel.changeStatus("Connecting...")
        broadcastStatus("Connecting...")
    }

    private fun createSocket() {
        try {
            val options = IO.Options().apply {
                forceNew = false
                multiplex = true
                transports = arrayOf(io.socket.engineio.client.transports.WebSocket.NAME)
                upgrade = true
                rememberUpgrade = false
                reconnection = true
                reconnectionAttempts = Int.MAX_VALUE
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                randomizationFactor = 0.5
                timeout = 20000

                path = "/otp/otp/socket.io"
            }
            mSocket = IO.socket(URI.create(defaultHost), options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun connectWebSocket() {
        mSocket?.apply {
            on(Socket.EVENT_CONNECT, onConnect)
            on(Socket.EVENT_DISCONNECT, onDisconnect)
            on(Socket.EVENT_CONNECT_ERROR, onConnectError)
            on(eventName, onNewMessage)
            connect()
        }
    }

    private val onConnect = Emitter.Listener {
        Log.d("SmsForegroundService", "Socket Connected!")
        viewModel.changeStatus("Connected")
        broadcastStatus("Connected")

//        mSocket?.emit("joinRoom","otp")
    }

    private val onDisconnect = Emitter.Listener {
        Log.d("SmsForegroundService", "Socket Disconnected!")
        viewModel.changeStatus("Disconnected")
        broadcastStatus("Disconnected")
    }

    private val onConnectError = Emitter.Listener {
        Log.e("SmsForegroundService", "Socket Connection Error!")
        viewModel.changeStatus("Connection error")
        broadcastStatus("Connection error")
    }

    private val onNewMessage = Emitter.Listener { args ->
        try {
            val obj = args[0] as JSONObject
            val phone = obj.getString("phoneNumber")
//            val phone = obj.getString("phone")
//            val otp = obj.getString("otp")
            val otp = obj.getString("code")
            sendSms(phone, otp)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendSms(phone: String, otp: String) {
        // Implement SMS sending logic here
        Log.d("SmsForegroundService", "Sending SMS to $phone with OTP: $otp")
        viewModel.sendSms(phone, otp)
    }

    override fun onDestroy() {
        super.onDestroy()
        disconnectWebSocket()
    }

    private fun disconnectWebSocket() {
        mSocket?.apply {
            off(Socket.EVENT_CONNECT, onConnect)
            off(Socket.EVENT_DISCONNECT, onDisconnect)
            off(Socket.EVENT_CONNECT_ERROR, onConnectError)
            off(eventName, onNewMessage)
            disconnect()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "SmsServiceChannel",
                "SMS Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        } else {

        }

    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "SmsServiceChannel")
            .setContentTitle("SMS Service")
            .setContentText("Managing socket connection and sending SMS.")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun broadcastStatus(status: String) {
        val intent = Intent("SOCKET_STATUS")
        intent.putExtra("status", status)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

}
