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

    private lateinit var mBinding: ActivityChatBinding
    private lateinit var mViewModel: ChatActivityViewModel
    private lateinit var mRvAdapter: RecyclerViewItemAdapter
    private lateinit var mRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityChatBinding.inflate(layoutInflater)
        val contentView = mBinding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE

        mViewModel = ChatActivityViewModel()
        mViewModel.setUsernameValue(intent.getStringExtra(USERNAME_EXTRA_NAME))

        if(!WebsocketService.isConnected) {
            WebsocketService.init()
        }

        runBlocking {
            val response = apolloClient.query(MessageListQuery()).execute()
            val rawMessages = response.data?.getAllMessages

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val listOfMessages = mViewModel.mapToRecyclerViewFormat(rawMessages, mViewModel.getUsernameValue())
                mViewModel.addEntireList(listOfMessages)
            }
        }

        mBinding.buttonSendMessage.setOnClickListener {
            val messageToSend = mBinding.output.text.toString()
            WebsocketService.sendMessage(messageToSend)
            mBinding.output.text.clear()
        }

        mBinding.buttonSeeMyMessageHistory.setOnClickListener {
            val intent = Intent(this, UserMessageHistoryActivity::class.java)
            intent.putExtra(USERNAME_EXTRA_NAME, mViewModel.getUsernameValue())
            intent.putExtra(USER_ID_EXTRA_NAME, mViewModel.getUserIdValue())
            startActivity(intent)
            finish()
        }

        WebsocketService.receivedUserId.observe(this) {
            mViewModel.setUserIdValue(WebsocketService.receivedUserId.value)
        }

        WebsocketService.receivedRecyclerViewItem.observe(this) {
            if (WebsocketService.receivedRecyclerViewItem.value!!.name != OBSERVER_LOCK) {
                mViewModel.addItemToList(WebsocketService.receivedRecyclerViewItem.value!!)
                mRvAdapter.notifyItemInserted(mViewModel.recyclerViewList.size - 1)
                mBinding.rvMessages.scrollToPosition(mRvAdapter.itemCount - 1)
            }
        }

        initRecyclerView()
        mBinding.rvMessages.scrollToPosition(mRvAdapter.itemCount - 1)
        val messageToSend = SEND_USERNAME_TO_SERVER_MESSAGE + "${mViewModel.getUsernameValue()}"
        WebsocketService.sendMessage(messageToSend)
    }

    private fun initRecyclerView() {
        mRecyclerView = mBinding.rvMessages
        mRvAdapter = RecyclerViewItemAdapter(mViewModel.recyclerViewList)
        mRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity)
            adapter = mRvAdapter
        }
        Log.d(TAG, "Recycler view initialized")
    }
}