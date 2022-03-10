package com.example.chatroom_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatroom_client.adapters.RecyclerViewItemAdapter
import com.example.chatroom_client.databinding.ActivityChatBinding
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
        private const val HOST_IP = "192.168.182.174"
        private const val PORT = 8080
        private const val PATH = "/chat"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatActivityViewModel
    private lateinit var rvAdapter: RecyclerViewItemAdapter
    private var messageToSend = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = "Chatroom"

        viewModel = ChatActivityViewModel()
        initRecyclerView()

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

        viewModel.messageCount.observe(this, {
            rvAdapter.notifyItemInserted(viewModel.recyclerViewList.size - 1)
            binding.rvMessages.scrollToPosition(rvAdapter.itemCount-1)
        })
    }

    private suspend fun runWebSocketClient(httpClient: HttpClient, hostIP: String, port: Int, path: String) {
        httpClient.ws(
            method = HttpMethod.Get,
            host = hostIP,
            port = port, path = path
        ) {
            Log.d(TAG, "Sucessfully connected!")

            val sendMessageJob = launch { sendMessage() }
            val receiveMessageJob = launch { listenForIncomingMessages(incoming) }
            receiveMessageJob.cancelAndJoin()
            sendMessageJob.join()
        }
        httpClient.close()
    }

    private suspend fun listenForIncomingMessages(incoming: ReceiveChannel<Frame>) {
        while(true) {
            try {
                when (val frame = incoming.receive()) {
                    is Frame.Text -> {
                        CoroutineScope(Dispatchers.Main).launch {
                            viewModel.addItemToList(frame.readText())
                        }
                        Log.d(TAG, "message received: ${frame.readText()}")
                    }
                    is Frame.Binary -> println(frame.readBytes())
                    else -> {
                        // Da spre da mrunka toq compiler
                    }
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

    private fun initRecyclerView(){
        val recyclerView = binding.rvMessages
        rvAdapter = RecyclerViewItemAdapter(viewModel.recyclerViewList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = rvAdapter
        }
    }
}