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
    private lateinit var mBinding: ActivityUserMessageHistoryBinding
    private lateinit var mRecyclerViewList: MutableList<RecyclerViewItemModel>
    private lateinit var mRvAdapter: RecyclerViewItemAdapter
    private lateinit var mRecyclerView: RecyclerView

    private var mUserId: Int? = null
    private var mUsername: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityUserMessageHistoryBinding.inflate(layoutInflater)
        val contentView = mBinding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE
        actionbar.setDisplayHomeAsUpEnabled(true)

        mUsername = intent.getStringExtra(USERNAME_EXTRA_NAME)
        mUserId = intent.getIntExtra(USER_ID_EXTRA_NAME, 10000)
        mRecyclerViewList = mutableListOf()

        //Each time the activity is launched, a GraphQL query is sent to the server,
        //to retrieve all messages that have been sent until now by the current user and to load them in the RV
        runBlocking {
            val response = apolloClient.query(GetAllMessagesByUserIdQuery(mUserId!!)).execute()
            val rawMessages = response.data?.getAllMessagesByUserId
            Log.d(TAG, "$rawMessages")

            if (rawMessages != null && rawMessages.isNotEmpty()) {
                mRecyclerViewList = mapToRecyclerViewFormat(rawMessages, mUsername)
                Log.d(TAG, "View model list: $mRecyclerViewList")
            }
        }
        initRecyclerView()
    }

    private fun initRecyclerView() {
        mRecyclerView = mBinding.rvMessageHistory
        mRvAdapter = RecyclerViewItemAdapter(mRecyclerViewList)
        mRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@UserMessageHistoryActivity)
            adapter = mRvAdapter
        }
        mBinding.rvMessageHistory.scrollToPosition(mRvAdapter.itemCount - 1)
        Log.d(TAG, "Recycler view initialized")
    }

    private fun mapToRecyclerViewFormat(
        rawMessages: List<GetAllMessagesByUserIdQuery.GetAllMessagesByUserId>?,
        username: String?
    ): MutableList<RecyclerViewItemModel> {
        val messagesListInRVFormat = rawMessages?.map {
            val name = "[$username]"
            val content = it.message.substring(name.length + 2, it.message.length)
            RecyclerViewItemModel(name = name, content = content)
        } as MutableList<RecyclerViewItemModel>

        Log.d(TAG, "Mapped to RV format")
        return messagesListInRVFormat
    }

    override fun onSupportNavigateUp(): Boolean {
        val intent = Intent(this, ChatActivity::class.java)
        intent.putExtra(USERNAME_EXTRA_NAME, mUsername)
        startActivity(intent)
        finish()
        return true
    }

    companion object {
        const val TAG = "MessageHistoryActivity"
        const val ACTIONBAR_TITLE = "My message history"
        const val USERNAME_EXTRA_NAME = "username"
        const val USER_ID_EXTRA_NAME = "userId"
    }
}