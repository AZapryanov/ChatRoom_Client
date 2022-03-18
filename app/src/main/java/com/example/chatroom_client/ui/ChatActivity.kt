package com.example.chatroom_client.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatroom_client.adapters.RecyclerViewItemAdapter
import com.example.chatroom_client.data.graphql.apolloClient
import com.example.chatroom_client.databinding.ActivityChatBinding
import com.example.chatroom_client.models.RecyclerViewItemModel
import com.example.chatroom_client.view_model.ChatActivityViewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import okhttp3.internal.notify
import src.main.graphql.MessageListQuery

class ChatActivity : AppCompatActivity() {
    companion object {
        const val TAG = "ChatActivity"
        private const val HOST_IP = "192.168.182.37"
        private const val PORT = 8080
        private const val PATH = "/chat"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatActivityViewModel
    private lateinit var rvAdapter: RecyclerViewItemAdapter
    private lateinit var recyclerView: RecyclerView
    private var messageToSend = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = "Chatroom"

        viewModel = ChatActivityViewModel()
        val username = intent.getStringExtra("username")

        runBlocking {
            val client = HttpClient(CIO) {
                install(WebSockets)
            }

            CoroutineScope(Dispatchers.IO).launch {
                runWebSocketClient(client, HOST_IP, PORT, PATH)
            }
        }

        runBlocking {
            val response = apolloClient.query(MessageListQuery()).execute()
            val rawMessages = response.data?.getAllMessages
            Log.d(TAG, "$rawMessages")

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val listOfMessages = mapToRecyclerViewFormat(rawMessages, username)
                viewModel.addEntireList(listOfMessages)
                Log.d(TAG, "View model list: ${viewModel.recyclerViewList}")
            }
        }

        binding.buttonSendMessage.setOnClickListener {
            messageToSend = binding.output.text.toString()
            binding.output.text.clear()
        }

        viewModel.messageCount.observe(this, {
            rvAdapter.notifyItemInserted(viewModel.recyclerViewList.size - 1)
            Log.d(TAG, "View model list: ${viewModel.recyclerViewList}")
            binding.rvMessages.scrollToPosition(rvAdapter.itemCount - 1)
        })

        initRecyclerView()
        messageToSend = "%: $username"
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
            Log.d(TAG, "Sucessfully connected!")

            val sendMessageJob = launch { sendMessage() }
            val receiveMessageJob = launch { listenForIncomingMessages(incoming) }
            receiveMessageJob.join()
            sendMessageJob.join()
        }
        httpClient.close()
    }

    private suspend fun listenForIncomingMessages(incoming: ReceiveChannel<Frame>) {
        Log.d(TAG, "Listening for incoming messages")
        while (true) {
            try {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            val message = frame.readText()
                            val name = message.substring(0, message.indexOf(':'))
                            val content = message.substring(name.length + 2, message.length)
                            Log.d(
                                TAG,
                                "Message received -> message: $message\nname: $name\ncontent: $content"
                            )
                            viewModel.addItemToList(name, content)
                        }
                        Log.d(TAG, "message received: ${frame.readText()}")
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
                Log.d(TAG, "Message sent: $messageToSend")
                messageToSend = ""
            }
        }
    }

    private fun initRecyclerView() {
        recyclerView = binding.rvMessages
        rvAdapter = RecyclerViewItemAdapter(viewModel.recyclerViewList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = rvAdapter
        }
        Log.d(TAG, "Recycler view initialized")
    }

    private suspend fun mapToRecyclerViewFormat(
        rawMessages: List<MessageListQuery.GetAllMessage>?,
        username: String?
    ): MutableList<RecyclerViewItemModel> {
        val messagesListInRVFormat = rawMessages?.map {

            var name = it.message.substring(1, it.message.indexOf(']'))
            val content = it.message.substring(name.length + 4, it.message.length)
            if (username == name.substring(0, name.length)) {
                name = "me"
            }
            RecyclerViewItemModel(name = name, content = content)
        } as MutableList<RecyclerViewItemModel>
        Log.d(TAG, "Mapped to RV format")
        return messagesListInRVFormat
    }
}