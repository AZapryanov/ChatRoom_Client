package com.example.chatroom_client.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatroom_client.adapters.RecyclerViewItemAdapter
import com.example.chatroom_client.connection.WebsocketService
import com.example.chatroom_client.data.graphql.apolloClient
import com.example.chatroom_client.databinding.ActivityChatBinding
import com.example.chatroom_client.view_model.ChatActivityViewModel
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.websocket.*
import io.ktor.http.*
import io.ktor.http.cio.websocket.*
import kotlinx.coroutines.*
import src.main.graphql.MessageListQuery

class ChatActivity : AppCompatActivity() {
    companion object {
        const val TAG = "ChatActivity"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatActivityViewModel
    private lateinit var rvAdapter: RecyclerViewItemAdapter
    private lateinit var recyclerView: RecyclerView
    private var client: HttpClient? = null

    var userId: Int? = null
    var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = "Chatroom"

        viewModel = ChatActivityViewModel()
        username = intent.getStringExtra("username")

        if(!WebsocketService.isConnected) {
            WebsocketService.init()
        }

        runBlocking {
            val response = apolloClient.query(MessageListQuery()).execute()
            val rawMessages = response.data?.getAllMessages
//            Log.d(TAG, "$rawMessages")

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val listOfMessages = viewModel.mapToRecyclerViewFormat(rawMessages, username)
                viewModel.addEntireList(listOfMessages)
//                Log.d(TAG, "View model list: ${viewModel.recyclerViewList}")
            }
        }

        binding.buttonSendMessage.setOnClickListener {
            val messageToSend = binding.output.text.toString()
            WebsocketService.sendMessage(messageToSend)
            binding.output.text.clear()
        }

        binding.buttonSeeMyMessageHistory.setOnClickListener {
            client?.cancel()
            val intent = Intent(this, UserMessageHistoryActivity::class.java)
            intent.putExtra("username", username)
            intent.putExtra("userId", userId)
            startActivity(intent)
            finish()
        }

        viewModel.messageCount.observe(this, {
            rvAdapter.notifyItemInserted(viewModel.recyclerViewList.size - 1)
//            Log.d(TAG, "View model list: ${viewModel.recyclerViewList}")
            binding.rvMessages.scrollToPosition(rvAdapter.itemCount - 1)
        }

        WebsocketService.receivedUserId.observe(this, {
            userId = WebsocketService.receivedUserId.value
        })

        WebsocketService.receivedRecyclerViewItem.observe(this, {
            viewModel.addItemToList(WebsocketService.receivedRecyclerViewItem.value!!)
        })

        initRecyclerView()
        val messageToSend = "%: $username"
        WebsocketService.sendMessage(messageToSend)
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
}