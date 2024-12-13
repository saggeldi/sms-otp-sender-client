package com.gonodono.smssender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.flowWithLifecycle
import com.gonodono.smssender.MainViewModel
import com.gonodono.smssender.UiState
import com.gonodono.smssender.data.Message
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@Composable
fun SmsScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val list = remember {
        mutableStateOf<List<Message>>(emptyList())
    }

    LaunchedEffect(true) {
        do {
            viewModel.getAllMessages {
                list.value = it
            }
            delay(5000)
        } while (true)
    }

    Column(
        modifier = modifier.background(
            color = Color.White
        )
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            items(list.value.count()) { index ->
                MessageUi(Modifier.fillMaxSize(), list.value[index])
            }
        }
    }
}

@Composable
fun MessageUi(modifier: Modifier = Modifier, message: Message) {
    Card(modifier = modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(8.dp)) {
            Text(message.address, style = MaterialTheme.typography.h6, color = Color.Black)
            Text(message.body, style = MaterialTheme.typography.body1, color = Color.Black)
            Text(message.sendStatus.toString(), style = MaterialTheme.typography.body1, color = Color.Black)
            Text(message.deliveryStatus.toString(), style = MaterialTheme.typography.body1, color = Color.Black)
        }
    }
}