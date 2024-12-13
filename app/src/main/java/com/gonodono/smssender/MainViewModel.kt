package com.gonodono.smssender

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonodono.smssender.data.Message
import com.gonodono.smssender.data.SendTask
import com.gonodono.smssender.repository.SmsSenderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject


sealed interface UiState {
    object Loading : UiState
    data class Active(
        val messages: String,
        val isSending: Boolean,
        val lastError: String?,
        val status: String
    ) : UiState
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: SmsSenderRepository
) : ViewModel() {
    private val _status = MutableStateFlow("pending")
    val status: StateFlow<String> = _status.asStateFlow()

    fun changeStatus(s: String) {
        _status.value = s
    }

    fun getAllMessages(onSuccess: (List<Message>) -> Unit) {
        viewModelScope.launch {
           val list =  repository.getListOfMessage()
            onSuccess(list)
        }
    }

    val uiState = combine(
        repository.allMessages,
        repository.latestSendTask
    ) { messages, task ->
        UiState.Active(
            itemize(messages),
            task?.state == SendTask.State.Running,
            task?.error,
            status = status.value
        )
    }

    fun queueDemoMessagesAndSend() {
        viewModelScope.launch {
            val messages = (ShortTexts + LongTexts).map { text ->
                Message("+99362737222", text, Message.SendStatus.Queued)
            }
            repository.insertMessagesAndSend(messages)
        }
    }

    fun sendSms(tel: String, message: String){
        viewModelScope.launch {
            val messages = listOf(Message(tel, message, Message.SendStatus.Queued))
            repository.insertMessagesAndSend(messages)
        }
    }

    fun resetFailedAndRetry() {
        viewModelScope.launch { repository.resetFailedAndRetry() }
    }
}

private fun itemize(messages: List<Message>) =
    messages.joinToString("\n\n") { it.toDebugString() }

private fun Message.toDebugString() =
    "#%d: to=%s, ss=%s, ds=%s sms=%s".format(id, address, sendStatus, deliveryStatus, body)

private val ShortTexts = arrayOf("Hi!", "Hello!", "Howdy!")

private val LongTexts = arrayOf(
    "У вас есть новый заказ, чтобы увидеть больше информации, нажмите здесь: Это просто проверка, не беспокойтесь об этом: 95.85.121.153:5577/",
    "Duis aute irure dolor in reprehenderit in voluptate velit esse cillum" +
            " dolore eu fugiat nulla pariatur. Excepteur sint occaecat" +
            " cupidatat non proident, sunt in culpa qui officia deserunt" +
            " mollit anim id est laborum.",
    "Sed ut perspiciatis unde omnis iste natus error sit voluptatem " +
            "accusantium doloremque laudantium, totam rem aperiam, eaque ipsa" +
            " quae ab illo inventore veritatis et quasi architecto beatae" +
            " vitae dicta sunt explicabo."
)

internal const val EMULATOR_PORT = "+99362737222"