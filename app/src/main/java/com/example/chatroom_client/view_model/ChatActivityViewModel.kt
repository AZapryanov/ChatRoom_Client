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

    fun addItemToList(item: String) {
        recyclerViewList.add(RecyclerViewItemModel(item))
        lastMessage = item
        messageCount.value = messageCount.value?.plus(1)
    }

    fun getMessageCount(): Int {
        return messageCount.value!!.toInt()
    }

}