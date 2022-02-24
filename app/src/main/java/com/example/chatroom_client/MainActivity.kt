package com.example.chatroom_client

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatroom_client.databinding.ActivityMainBinding
import com.example.chatroom_client.view_model.MainActivityViewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatApp"
        private const val HOST_IP = "192.168.182.37"
        private const val PORT = 8080
        private const val PATH = "/chat"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel
    private lateinit var messageToSend: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        viewModel = MainActivityViewModel()
        messageToSend = ""

        val client = HttpClient(CIO) {
            install(WebSockets)
        }

        CoroutineScope(Dispatchers.IO).launch {
            runWebSocketClient(client, HOST_IP, PORT, PATH)
        }

        binding.buttonSendMessage.setOnClickListener {
            messageToSend = binding.output.text.toString()
        }

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
                        binding.input.text = frame.readText()
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