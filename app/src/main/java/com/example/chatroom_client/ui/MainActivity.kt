package com.example.chatroom_client.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.chatroom_client.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        const val ACTIONBAR_TITLE = "User registration"
        const val USERNAME_EXTRA_NAME = "username"
    }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val contentView = binding.root
        setContentView(contentView)

        val actionbar = supportActionBar
        actionbar!!.title = ACTIONBAR_TITLE

        binding.buttonJoinChatroom.setOnClickListener {
            val username = binding.etEnterUsername.text.toString()

            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra(USERNAME_EXTRA_NAME, username)
            startActivity(intent)
            finish()
        }
    }
}