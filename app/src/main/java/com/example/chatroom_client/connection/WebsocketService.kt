package com.example.chatroom_client.connection

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.example.chatroom_client.models.RecyclerViewItemModel
import com.example.chatroom_client.ui.ChatActivity
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object WebsocketService {
    private const val HOST_IP = "192.168.182.37"
    private const val PORT = 8080
    private const val PATH = "/chat"

    var isConnected = false

    val receivedUserId: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val receivedRecyclerViewItem: MutableLiveData<RecyclerViewItemModel> by lazy {
        MutableLiveData<RecyclerViewItemModel>()
    }

    private var client: HttpClient? = null
    private var messageToSend = ""

    fun init() {
        runBlocking {
            client = HttpClient(CIO) {
                install(WebSockets)
            }

            CoroutineScope(Dispatchers.IO).launch {
                runWebSocketClient(client!!, HOST_IP, PORT, PATH)
            }
        }
    }

    private suspend fun runWebSocketClient(
        httpClient: HttpClient,
        hostIP: String,
        port: Int,
        path: String
    ) {
        httpClient.ws(
            method = HttpMethod.Get,
            host = hostIP,
            port = port, path = path
        ) {
            Log.d(ChatActivity.TAG, "Sucessfully connected!")
            isConnected = true

            val sendMessageJob = launch { sendMessage() }
            val receiveMessageJob = launch { listenForIncomingMessages(incoming) }
            receiveMessageJob.join()
            sendMessageJob.join()
        }
        isConnected = false
        httpClient.close()
    }

    private suspend fun listenForIncomingMessages(incoming: ReceiveChannel<Frame>) {
        Log.d(ChatActivity.TAG, "Listening for incoming messages")
        while (true) {
            try {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            val message = frame.readText()
                            if (message.startsWith("%")) {
                                receivedUserId.value = message.subSequence(2, message.length).toString().toInt()
                            } else {
                                val name = message.substring(0, message.indexOf(':'))
                                val content = message.substring(name.length + 2, message.length)
                                Log.d(
                                    ChatActivity.TAG,
                                    "Message received -> message: $message\nname: $name\ncontent: $content"
                                )
                                receivedRecyclerViewItem.value = RecyclerViewItemModel(name, content)
                                receivedRecyclerViewItem.value = RecyclerViewItemModel(ChatActivity.OBSERVER_LOCK, ChatActivity.OBSERVER_LOCK)
                            }
                        }
                        Log.d(ChatActivity.TAG, "message received: ${frame.readText()}")
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
                //Log.w(TAG, "Failure: ${e.message}")
            } catch (e: Exception) {
                //Log.w(TAG, "Failure: ${e.message}")
            }
        }
    }

    private suspend fun WebSocketSession.sendMessage() {
        while (true) {
            if (messageToSend.isNotEmpty()) {
                send(Frame.Text(messageToSend))
                Log.d(ChatActivity.TAG, "Message sent: $messageToSend")
                messageToSend = ""
            }
        }
    }

    fun sendMessage(message: String) {
        this.messageToSend = message
    }
}