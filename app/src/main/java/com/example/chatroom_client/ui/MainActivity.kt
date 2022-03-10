package com.example.chatroom_client.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.chatroom_client.data.graphql.apolloClient
import com.example.chatroom_client.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import src.main.graphql.MessageListQuery

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = "Chat Application"

        //Test GraphQL------------------------------------------------------
        CoroutineScope(Dispatchers.IO).launch {
            val response = apolloClient.query(MessageListQuery()).execute()
            val getAllMessageList = response.data?.getAllMessages
            val messageAsData = getAllMessageList?.get(0)
            val message = messageAsData?.message
            Log.d("MessageList", "Success ${response.data}")
            Log.d("MessageList", "Message as data: $messageAsData")
            Log.d("MessageList", "Only message text: $message")

            if (getAllMessageList != null) {
                for (message in getAllMessageList) {
                    Log.d("MessageList", message.message)
                }
            }
        }
//        ------------------------------------------------------------------

        binding.buttonJoinChatroom.setOnClickListener {
            val username = binding.etEnterUsername.text.toString()

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
        }
    }
}