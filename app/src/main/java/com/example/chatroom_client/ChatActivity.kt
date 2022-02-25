package com.example.chatroom_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatroom_client.adapters.RecyclerViewItemAdapter
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

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
        private const val HOST_IP = "192.168.182.37"
        private const val PORT = 8080
        private const val PATH = "/chat"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatActivityViewModel
    private lateinit var messageToSend: String

    private lateinit var recyclerView: RecyclerView
    private lateinit var items: MutableList<RecyclerViewItemModel>
    private lateinit var rvAdapter: RecyclerViewItemAdapter

    private val messageToAddToRecyclerView: MutableLiveData<String> by lazy{
        MutableLiveData<String>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = "Chatroom"

        viewModel = ChatActivityViewModel()
        messageToSend = ""

//        recyclerView = binding.rvMessages
        items = mutableListOf(RecyclerViewItemModel("me: Hey"), RecyclerViewItemModel("Alex: yo"), RecyclerViewItemModel("Gogo: wazzup"))


        val client = HttpClient(CIO) {
            install(WebSockets)
        }

        CoroutineScope(Dispatchers.IO).launch {
            runWebSocketClient(client, HOST_IP, PORT, PATH)
        }

        val username = intent.getStringExtra("username")
        messageToSend = "%: $username"

        binding.buttonSendMessage.setOnClickListener {
            messageToSend = binding.output.text.toString()
        }

        messageToAddToRecyclerView.observe(this, {
            val template: String? = if (messageToAddToRecyclerView.value.toString().subSequence(0, 2) == "me") {
                "my"
            } else {
                "others"
            }
//            items.add(RecyclerViewItemModel(messageToAddToRecyclerView.value))
////            items.add(RecyclerViewItemModel(messageToAddToRecyclerView.value))
            val recyclerView = binding.rvMessages
            val rvItems = items
            val rvAdapter = RecyclerViewItemAdapter(rvItems, template)
            recyclerView.apply {
                layoutManager = LinearLayoutManager(this@ChatActivity)
                recyclerView.adapter = rvAdapter
            }

//
//            rvAdapter = RecyclerViewItemAdapter(items, template)
//
//            recyclerView.apply {
//                layoutManager = LinearLayoutManager(this@ChatActivity)
//                recyclerView.adapter = rvAdapter
//            }
        })

        messageToAddToRecyclerView.value = "me: Hey"
//
        messageToAddToRecyclerView.value = "Alex: yo"
//
//        messageToAddToRecyclerView.value = "Gogo: wazzup"
//
//        messageToAddToRecyclerView.value = "me: fff"
//
//        messageToAddToRecyclerView.value = "me: eee"
    }

    private suspend fun runWebSocketClient(httpClient: HttpClient, hostIP: String, port: Int, path: String) {
        httpClient.ws(
            method = HttpMethod.Get,
            host = hostIP,
            port = port, path = path
        ) {
            Log.d(TAG, "Sucessfully connected!")
            val sendMessageJob = launch {
                sendMessage()
            }
            val receiveMessageJob = launch { listenForIncomingMessages(incoming) }

            receiveMessageJob.cancelAndJoin()
            sendMessageJob.join()
        }
        httpClient.close()
    }

    private suspend fun listenForIncomingMessages(incoming: ReceiveChannel<Frame>) {
        while(true) {
            try {
                val frame = incoming.receive()
                when (frame) {
                    is Frame.Text -> {
                        GlobalScope.launch(Dispatchers.Main) {
                            items.add(RecyclerViewItemModel(frame.readText()))
                            messageToAddToRecyclerView.value = frame.readText()
                        }
                        Log.d(TAG, "message received: ${frame.readText()}")
                    }
                    is Frame.Binary -> println(frame.readBytes())
                }
            } catch (e: ClosedReceiveChannelException) {
                Log.w(TAG, "Failure: ${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "Failure: ${e.message}")
            }
        }
    }

    private suspend fun WebSocketSession.sendMessage() {
        while(true) {
            if (messageToSend.isNotEmpty()) {
                send(Frame.Text(messageToSend))
                messageToSend = ""
            }
        }
    }
}