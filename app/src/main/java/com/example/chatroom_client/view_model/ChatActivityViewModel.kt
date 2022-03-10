package com.example.chatroom_client.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatroom_client.models.RecyclerViewItemModel

class ChatActivityViewModel: ViewModel() {
    val recyclerViewList: MutableList<RecyclerViewItemModel> = mutableListOf()
    var lastMessage: String = ""

    val messageCount: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun addItemToList(name: String, content: String) {
        recyclerViewList.add(RecyclerViewItemModel(name, content))
        lastMessage = "[$name]: $content"
        messageCount.value = messageCount.value?.plus(1)
    }
}