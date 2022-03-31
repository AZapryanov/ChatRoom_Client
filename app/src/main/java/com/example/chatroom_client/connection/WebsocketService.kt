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

    private const val TAG = "WebsocketService"
    private const val HOST_IP = "192.168.182.37"
    private const val PORT = 8080
    private const val PATH = "/chat"
    private const val RECEIVED_USER_ID_KEY_CHAR = "%"
    private const val END_OF_USERNAME_KEY_CHAR = ':'

    val mReceivedUserId: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    val mReceivedRecyclerViewItem: MutableLiveData<RecyclerViewItemModel> by lazy {
        MutableLiveData<RecyclerViewItemModel>()
    }

    var mIsConnected = false
    private var mClient: HttpClient? = null
    private var mMessageToSend = ""

    fun init() {
        //It is important that runBlocking is used here, because sometimes in the ChatActivity
        //the Recycler view gets initialized before the websocket client is finished setting up
        //and this triggers some bugs
        runBlocking {
            mClient = HttpClient(CIO) {
                install(WebSockets)
            }

            CoroutineScope(Dispatchers.IO).launch {
                runWebSocketClient(mClient!!, HOST_IP, PORT, PATH)
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
            mIsConnected = true

            val sendMessageJob = launch { sendMessage() }
            val receiveMessageJob = launch { listenForIncomingMessages(incoming) }
            receiveMessageJob.join()
            sendMessageJob.join()
        }
        mIsConnected = false
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

                            //If the received message starts with this key it means that the server has sent the current user's ID,
                            //from the database and it needs to be stored - It is used for GraphQL calls
                            if (message.startsWith(RECEIVED_USER_ID_KEY_CHAR)) {
                                mReceivedUserId.value = message.subSequence(2, message.length).toString().toInt()
                            } else {

                                //There are two fields in the RV templates, that need to be populated,
                                //hence each received message has to be divided in two parts
                                val name = message.substring(0, message.indexOf(END_OF_USERNAME_KEY_CHAR))
                                val content = message.substring(name.length + 2, message.length)
                                Log.d(
                                    ChatActivity.TAG,
                                    "Message received -> message: $message\nname: $name\ncontent: $content"
                                )
                                mReceivedRecyclerViewItem.value = RecyclerViewItemModel(name, content)
                                mReceivedRecyclerViewItem.value = RecyclerViewItemModel(ChatActivity.OBSERVER_LOCK, ChatActivity.OBSERVER_LOCK)
                            }
                        }
                        Log.d(ChatActivity.TAG, "message received: ${frame.readText()}")
                    }
                }
            } catch (e: ClosedReceiveChannelException) {
//                Log.w(TAG, "Failure: ${e.message}")
            } catch (e: Exception) {
//                Log.w(TAG, "Failure: ${e.message}")
            }
        }
    }

    private suspend fun WebSocketSession.sendMessage() {
        while (true) {
            if (mMessageToSend.isNotEmpty()) {
                send(Frame.Text(mMessageToSend))
                Log.d(ChatActivity.TAG, "Message sent: $mMessageToSend")
                mMessageToSend = ""
            }
        }
    }

    fun sendMessage(message: String) {
        this.mMessageToSend = message
    }
}