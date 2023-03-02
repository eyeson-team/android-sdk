package com.eyeson.android.ui.components

import androidx.annotation.FloatRange
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.animation.slideOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.End
import androidx.compose.ui.Alignment.Companion.Top
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.eyeson.android.R
import com.eyeson.android.ui.meeting.ChatMessage
import com.eyeson.android.ui.meeting.ChatMessage.IncomingMessage
import com.eyeson.android.ui.meeting.ChatMessage.OutgoingMessage
import com.eyeson.android.ui.theme.ChatHeader
import com.eyeson.android.ui.theme.ChatMessage
import com.eyeson.android.ui.theme.DarkGray900
import com.eyeson.android.ui.theme.EyesonDemoTheme
import timber.log.Timber
import java.text.DecimalFormat
import java.util.*

@Composable
fun Chat(
    visible: Boolean,
    onClose: () -> Unit,
    messages: List<ChatMessage>,
    modifier: Modifier = Modifier,
    sendMessage: (String) -> Unit,
    title: String = stringResource(id = R.string.chat).uppercase(),
    scrimColor: Color = MaterialTheme.colors.onSurface.copy(alpha = 0.40f),
    @FloatRange(from = 0.0, to = 1.0) horizontalContentRatio: Float = 1.0f,
    @FloatRange(from = 0.0, to = 1.0) verticalContentRatio: Float = 1.0f,
    contentShape: Shape = MaterialTheme.shapes.large,
    contentBackgroundColor: Color = MaterialTheme.colors.surface
) {
    var textState by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }

    BoxWithConstraints(modifier) {
        Scrim(scrimColor, visible, onClose)

        Column(
            modifier = Modifier
                .fillMaxWidth(horizontalContentRatio)
                .fillMaxHeight(verticalContentRatio)
                .wrapContentWidth(Alignment.CenterHorizontally)
                .align(BottomEnd)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideIn { fullSize ->
                    IntOffset(fullSize.width / 4, 100)
                } + fadeIn(),
                exit = slideOut { fullSize ->
                    IntOffset(fullSize.width / 4, 100)
                } + fadeOut(),
                modifier = modifier
            ) {
                Surface(shape = contentShape, color = contentBackgroundColor) {
                    Column(verticalArrangement = Arrangement.Bottom) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween

                        ) {
                            Text(
                                modifier = modifier.padding(start = 16.dp),
                                text = title,
                                style = MaterialTheme.typography.h1
                            )
                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    stringResource(id = R.string.close_menu),
                                    tint = contentColorFor(contentBackgroundColor)
                                )
                            }
                        }
                        Divider(Modifier.background(color = contentBackgroundColor))

                        LazyColumn(
                            reverseLayout = true, modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            items(messages) { message ->
                                when (message) {
                                    is OutgoingMessage -> {
                                        ChatMessageOutgoing(message.text, message.time)
                                    }
                                    is IncomingMessage -> {
                                        ChatMessageIncoming(
                                            text = message.text,
                                            from = message.from,
                                            time = message.time,
                                            avatarUrl = message.avatarUrl
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(Modifier.background(color = contentBackgroundColor))
                        UserInputText(
                            textFieldValue = textState,
                            onTextChanged = { textState = it },
                            onMessageSend = {
                                sendMessage(textState.text.trim())
                                textState = TextFieldValue()
                            }
                        )

                    }
                }
            }
        }
    }
}

private val IncomingBubbleShape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 16.dp)
private val OutgoingBubbleShape = RoundedCornerShape(16.dp, 0.dp, 16.dp, 16.dp)

@Composable
fun ChatMessageIncoming(
    text: String,
    from: String,
    time: Date,
    modifier: Modifier = Modifier,
    avatarUrl: String? = null
) {
    Column(modifier = modifier.padding(start = 16.dp, end = 16.dp)) {
        Row(verticalAlignment = CenterVertically) {
            SubcomposeAsyncImage(
                model = avatarUrl,
                loading = {
                    Surface(
                        shape = CircleShape,
                        color = Color.Gray
                    ) { /** intentionally empty **/ }
                },
                contentDescription = stringResource(R.string.avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp, 40.dp)
                    .clip(CircleShape)
                    .align(Top)
            )
            val format = DecimalFormat("00")

            Text(
                text = from.uppercase(),
                modifier = Modifier.padding(start = 12.dp),
                style = ChatHeader
            )
            Text(
                text = "${format.format(time.hours)}:${format.format(time.minutes)}",
                modifier = Modifier.padding(start = 4.dp),
                style = ChatHeader.copy(
                    color = ChatHeader.color.copy(alpha = 0.6f)
                )
            )
        }
        Surface(
            shape = IncomingBubbleShape,
            color = DarkGray900,
            modifier = Modifier.padding(start = 52.dp)
        ) {
            Text(text = text, style = ChatMessage, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
fun ChatMessageOutgoing(
    text: String,
    time: Date,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(start = 16.dp, end = 16.dp)
            .fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            val format = DecimalFormat("00")
            Text(
                text = "${format.format(time.hours)}:${format.format(time.minutes)}",
                modifier = Modifier.padding(end = 4.dp),
                style = ChatHeader.copy(
                    color = ChatHeader.color.copy(alpha = 0.6f)
                )
            )
            Text(
                text = stringResource(R.string.you).uppercase(),
                style = ChatHeader
            )

        }
        Surface(
            shape = OutgoingBubbleShape,
            color = DarkGray900.copy(alpha = 0.8f),
            modifier = Modifier
                .padding(start = 52.dp, top = 8.dp)
                .align(End)
        ) {
            Text(text = text, style = ChatMessage, modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun UserInputText(
    onTextChanged: (TextFieldValue) -> Unit,
    textFieldValue: TextFieldValue,
    onMessageSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .align(Alignment.Bottom)
                    .weight(1f)
                ,
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { onTextChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .align(Alignment.CenterStart),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(onSend = {
                        if (textFieldValue.text.isNotBlank()) {
                            onMessageSend()
                        }
                    }),
                    textStyle = MaterialTheme.typography.body1.copy(fontSize = 16.sp)
                )

                if (textFieldValue.text.isEmpty()) {
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp),
                        text = stringResource(id = R.string.type_message),
                        style = MaterialTheme.typography.body1.copy(
                            fontSize = 16.sp,
                            color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    )
                }
            }
            IconButton(
                onClick = onMessageSend,
                enabled = textFieldValue.text.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    stringResource(id = R.string.close_menu),
                )
            }
        }
    }
}

private val messages = listOf(
    IncomingMessage(
        text = "Hello there!",
        from = "Karen Miller",
        time = Date(),
        avatarUrl = "https://picsum.photos/300/300"
    ),
    OutgoingMessage(
        "Hello there! How are you doing? Please let me know when you want to follow up.",
        Date()
    ),
    IncomingMessage(
        text = "Let’s meet again tomorrow!",
        from = "Karen Miller",
        time = Date(),
        avatarUrl = "https://picsum.photos/300/300"
    ),
)

@Preview()
@Composable
fun ChatMessageIncomingPreview() {
    EyesonDemoTheme {
        ChatMessageIncoming(
            text = "Hello there! How are you doing? Please let me know when you want to follow up.",
            from = "Karen Miller",
            time = Date(),
            avatarUrl = "https://picsum.photos/300/300"
        )
    }
}

@Preview()
@Composable
fun ChatMessageOutgoingPreview() {
    EyesonDemoTheme {
        ChatMessageOutgoing(
            text = "Let’s meet again tomorrow!",
            time = Date()
        )
    }
}

@Preview()
@Composable
fun ChatPreview() {
    EyesonDemoTheme {
        Chat(
            true,
            {},
            messages,
            contentShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            verticalContentRatio = 0.7f,
            sendMessage = { Timber.d("Send: $it") }
        )
    }
}

