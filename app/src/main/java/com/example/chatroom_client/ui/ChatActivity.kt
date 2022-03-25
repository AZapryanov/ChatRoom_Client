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
        const val ACTIONBAR_TITLE = "Chatroom"
        const val OBSERVER_LOCK = "^&$#"
        const val USERNAME_EXTRA_NAME = "username"
        const val USER_ID_EXTRA_NAME = "userId"
        const val SEND_USERNAME_TO_SERVER_MESSAGE = "%: "
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var viewModel: ChatActivityViewModel
    private lateinit var rvAdapter: RecyclerViewItemAdapter
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE

        viewModel = ChatActivityViewModel()
        viewModel.setUsernameValue(intent.getStringExtra(USERNAME_EXTRA_NAME))

        if(!WebsocketService.isConnected) {
            WebsocketService.init()
        }

        runBlocking {
            val response = apolloClient.query(MessageListQuery()).execute()
            val rawMessages = response.data?.getAllMessages

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val listOfMessages = viewModel.mapToRecyclerViewFormat(rawMessages, viewModel.getUsernameValue())
                viewModel.addEntireList(listOfMessages)
            }
        }

        binding.buttonSendMessage.setOnClickListener {
            val messageToSend = binding.output.text.toString()
            WebsocketService.sendMessage(messageToSend)
            binding.output.text.clear()
        }

        binding.buttonSeeMyMessageHistory.setOnClickListener {
            val intent = Intent(this, UserMessageHistoryActivity::class.java)
            intent.putExtra(USERNAME_EXTRA_NAME, viewModel.getUsernameValue())
            intent.putExtra(USER_ID_EXTRA_NAME, viewModel.getUserIdValue())
            startActivity(intent)
            finish()
        }

        WebsocketService.receivedUserId.observe(this) {
            viewModel.setUserIdValue(WebsocketService.receivedUserId.value)
        }

        WebsocketService.receivedRecyclerViewItem.observe(this) {
            if (WebsocketService.receivedRecyclerViewItem.value!!.name != OBSERVER_LOCK) {
                viewModel.addItemToList(WebsocketService.receivedRecyclerViewItem.value!!)
                rvAdapter.notifyItemInserted(viewModel.recyclerViewList.size - 1)
                binding.rvMessages.scrollToPosition(rvAdapter.itemCount - 1)
            }
        }

        initRecyclerView()
        binding.rvMessages.scrollToPosition(rvAdapter.itemCount - 1)
        val messageToSend = SEND_USERNAME_TO_SERVER_MESSAGE + "${viewModel.getUsernameValue()}"
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