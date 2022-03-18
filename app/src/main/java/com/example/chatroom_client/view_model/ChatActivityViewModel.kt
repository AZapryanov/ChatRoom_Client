package com.example.chatroom_client.view_model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatroom_client.models.RecyclerViewItemModel
import com.example.chatroom_client.ui.ChatActivity
import kotlinx.coroutines.Dispatchers

class ChatActivityViewModel: ViewModel() {
    var recyclerViewList: MutableList<RecyclerViewItemModel> = mutableListOf()

    val messageCount: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    suspend fun addItemToList(name: String, content: String) {
        recyclerViewList.add(RecyclerViewItemModel(name, content))
        Log.d(ChatActivity.TAG, "Message added to RV list")
        messageCount.value = messageCount.value?.plus(1)
    }

    suspend fun addEntireList(list: MutableList<RecyclerViewItemModel>) {
        recyclerViewList = list
        increaseCountByListLength(list.size)
        Log.d(ChatActivity.TAG, "Entire list added")
    }

    suspend fun increaseCountByListLength(listLength: Int) {
        messageCount.value = messageCount.value?.plus(listLength)
        Log.d(ChatActivity.TAG, "Count increased by list size")
    }
}