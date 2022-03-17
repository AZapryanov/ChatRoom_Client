package com.example.chatroom_client.view_model

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatroom_client.models.RecyclerViewItemModel
import kotlinx.coroutines.Dispatchers

class ChatActivityViewModel: ViewModel() {
    var recyclerViewList: MutableList<RecyclerViewItemModel> = mutableListOf()

    val messageCount: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    suspend fun addItemToList(name: String, content: String) {
        recyclerViewList.add(RecyclerViewItemModel(name, content))
        messageCount.value = messageCount.value?.plus(1)
    }

    suspend fun addEntireList(list: MutableList<RecyclerViewItemModel>) {
        recyclerViewList = list
        increaseCountByListLength(list.size)
    }

    suspend fun increaseCountByListLength(listLength: Int) {
        messageCount.value = messageCount.value?.plus(listLength)
    }
}