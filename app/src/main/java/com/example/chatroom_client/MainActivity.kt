package com.example.chatroom_client

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val client = HttpClient(CIO) {
            install(WebSockets)
        }

        runBlocking {
            client.ws(
                method = HttpMethod.Get,
                host = "127.0.0.1",
                port = 8080, path = "/ws"
            ) {
                send(Frame.Text("Ping to webserver"))
                try {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> Log.i(TAG, "message received: ${frame.readText()}")
                        is Frame.Binary -> println(frame.readBytes())
                    }
                } catch (e: ClosedReceiveChannelException) {
                    Log.w(TAG, "Failure: ${e.message}")
                } catch (e: Exception) {
                    Log.w(TAG, "Failure: ${e.message}")
                }
            }
        }
    }
}