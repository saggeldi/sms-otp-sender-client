package com.gonodono.smssender.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val socketStatus by viewModel.status.collectAsState()

    LaunchedEffect(true) {
        do {
            viewModel.getAllMessages {
                list.value = it
            }
            delay(5000)
        } while (true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(0xFF121212))
    ) {
        // Enhanced Status Header
        StatusHeader(socketStatus = socketStatus, messageCount = list.value.size)

        // Messages List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        ) {
            items(list.value) { message ->
                MessageUi(message = message)
            }

            // Empty state
            if (list.value.isEmpty()) {
                item {
                    EmptyState()
                }
            }
        }
    }
}

@Composable
fun StatusHeader(socketStatus: String, messageCount: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp,
        color = Color(0xFF1E1E1E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Socket Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Indicator
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (socketStatus) {
                                "Connected" -> Color(0xFF4CAF50)
                                "Disconnected" -> Color(0xFF757575)
                                "Connection Error" -> Color(0xFFE53935)
                                else -> Color(0xFFFF9800)
                            }
                        )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Socket Connection",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFFBBBBBB)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = socketStatus,
                    style = MaterialTheme.typography.body1,
                    fontWeight = FontWeight.Medium,
                    color = when (socketStatus) {
                        "Connected" -> Color(0xFF4CAF50)
                        "Disconnected" -> Color(0xFF757575)
                        "Connection Error" -> Color(0xFFE53935)
                        else -> Color(0xFFFF9800)
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message Count Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Messages",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFFBBBBBB)
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = "$messageCount",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE0E0E0)
                )
            }
        }
    }
}

@Composable
fun MessageUi(modifier: Modifier = Modifier, message: Message) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = 2.dp,
        backgroundColor = Color(0xFF2A2A2A)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with phone number and status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.address,
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFE0E0E0),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Status badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    StatusBadge(
                        label = "Send",
                        status = message.sendStatus.toString()
                    )
                    StatusBadge(
                        label = "Delivery",
                        status = message.deliveryStatus.toString()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Message body
            Text(
                text = message.body,
                style = MaterialTheme.typography.body1,
                color = Color(0xFFCCCCCC),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Divider(color = Color(0xFF404040), thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            // Status details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusDetail(
                    label = "Send Status",
                    value = message.sendStatus.toString(),
                    isSuccess = message.sendStatus.toString().contains("SUCCESS", ignoreCase = true)
                )

                StatusDetail(
                    label = "Delivery Status",
                    value = message.deliveryStatus.toString(),
                    isSuccess = message.deliveryStatus.toString().contains("SUCCESS", ignoreCase = true)
                )
            }
        }
    }
}

@Composable
fun StatusBadge(label: String, status: String) {
    val backgroundColor = when {
        status.contains("SUCCESS", ignoreCase = true) -> Color(0xFF1B5E20)
        status.contains("FAILED", ignoreCase = true) || status.contains("ERROR", ignoreCase = true) -> Color(0xFF8B0000)
        status.contains("PENDING", ignoreCase = true) -> Color(0xFF4A2C00)
        else -> Color(0xFF3A3A3A)
    }

    val textColor = when {
        status.contains("SUCCESS", ignoreCase = true) -> Color(0xFF81C784)
        status.contains("FAILED", ignoreCase = true) || status.contains("ERROR", ignoreCase = true) -> Color(0xFFE57373)
        status.contains("PENDING", ignoreCase = true) -> Color(0xFFFFB74D)
        else -> Color(0xFFBBBBBB)
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun StatusDetail(label: String, value: String, isSuccess: Boolean) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = Color(0xFF999999),
            fontSize = 10.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.body2,
            color = if (isSuccess) Color(0xFF81C784) else Color(0xFFBBBBBB),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No messages yet",
                style = MaterialTheme.typography.h6,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Messages will appear here when available",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF666666)
            )
        }
    }
}