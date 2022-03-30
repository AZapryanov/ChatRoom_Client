package com.example.chatroom_client.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatroom_client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityMainBinding.inflate(layoutInflater)
        val contentView = mBinding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE

        mBinding.buttonJoinChatroom.setOnClickListener {
            val username = mBinding.etEnterUsername.text.toString()
            val intent = Intent(this, ChatActivity::class.java)

            //Send the username to the ChatActivity, where it will
            //be sent to the server once the Websocket is open
            intent.putExtra(USERNAME_EXTRA_NAME, username)
            startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        const val ACTIONBAR_TITLE = "User registration"
        const val USERNAME_EXTRA_NAME = "username"
    }
}