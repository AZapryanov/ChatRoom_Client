package com.example.chatroom_client.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatroom_client.adapters.RecyclerViewItemAdapter
import com.example.chatroom_client.data.graphql.apolloClient
import com.example.chatroom_client.databinding.ActivityUserMessageHistoryBinding
import com.example.chatroom_client.models.RecyclerViewItemModel
import kotlinx.coroutines.runBlocking
import src.main.graphql.GetAllMessagesByUserIdQuery

class UserMessageHistoryActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MessageHistoryActivity"
        const val ACTIONBAR_TITLE = "My message history"
        const val USERNAME_EXTRA_NAME = "username"
        const val USER_ID_EXTRA_NAME = "userId"
    }

    private lateinit var binding: ActivityUserMessageHistoryBinding
    private lateinit var recyclerViewList: MutableList<RecyclerViewItemModel>
    private lateinit var rvAdapter: RecyclerViewItemAdapter
    private lateinit var recyclerView: RecyclerView

    private var userId: Int? = null
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserMessageHistoryBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE

        username = intent.getStringExtra(USERNAME_EXTRA_NAME)
        userId = intent.getIntExtra(USER_ID_EXTRA_NAME, 10000)
        recyclerViewList = mutableListOf()

        Log.d(TAG, "$userId")
        binding.buttonBackToChat.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(USERNAME_EXTRA_NAME, username)
            startActivity(intent)
            finish()
        }

        runBlocking {
            val response = apolloClient.query(GetAllMessagesByUserIdQuery(userId!!)).execute()
            val rawMessages = response.data?.getAllMessagesByUserId
            Log.d(TAG, "$rawMessages")

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                val listOfMessages = mapToRecyclerViewFormat(rawMessages, username)
                recyclerViewList = listOfMessages
                Log.d(TAG, "View model list: $recyclerViewList")
            }
        }

        initRecyclerView()
    }

    private fun initRecyclerView() {
        recyclerView = binding.rvMessageHistory
        rvAdapter = RecyclerViewItemAdapter(recyclerViewList)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserMessageHistoryActivity)
            adapter = rvAdapter
        }
        binding.rvMessageHistory.scrollToPosition(rvAdapter.itemCount - 1)
        Log.d(TAG, "Recycler view initialized")
    }

    private suspend fun mapToRecyclerViewFormat(
        rawMessages: List<GetAllMessagesByUserIdQuery.GetAllMessagesByUserId>?,
        username: String?
    ): MutableList<RecyclerViewItemModel> {
        val messagesListInRVFormat = rawMessages?.map {
            val name = "[$username]"
            val content = it.message.substring(name!!.length + 2, it.message.length)
            RecyclerViewItemModel(name = name, content = content)
        } as MutableList<RecyclerViewItemModel>

        Log.d(TAG, "Mapped to RV format")
        return messagesListInRVFormat
    }
}