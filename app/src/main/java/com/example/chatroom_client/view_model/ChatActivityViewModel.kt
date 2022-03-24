package com.example.chatroom_client.view_model

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.chatroom_client.models.RecyclerViewItemModel
import com.example.chatroom_client.ui.ChatActivity
import src.main.graphql.MessageListQuery

class ChatActivityViewModel: ViewModel() {
    var recyclerViewList: MutableList<RecyclerViewItemModel> = mutableListOf()

    val messageCount: MutableLiveData<Int> by lazy {
        MutableLiveData<Int>()
    }

    fun addItemToList(itemToAdd: RecyclerViewItemModel) {
        recyclerViewList.add(itemToAdd)
        Log.d(ChatActivity.TAG, "Message added to RV list")
        messageCount.value = messageCount.value?.plus(1)
    }

    fun addEntireList(list: MutableList<RecyclerViewItemModel>) {
        recyclerViewList = list
        increaseCountByListLength(list.size)
        Log.d(ChatActivity.TAG, "Entire list added")
    }

    private fun increaseCountByListLength(listLength: Int) {
        messageCount.value = messageCount.value?.plus(listLength)
        Log.d(ChatActivity.TAG, "Count increased by list size")
    }

    fun mapToRecyclerViewFormat(
        rawMessages: List<MessageListQuery.GetAllMessage>?,
        username: String?
    ): MutableList<RecyclerViewItemModel> {
        val messagesListInRVFormat = rawMessages?.map {
            var name = it.message.substring(0, it.message.indexOf(':'))
            val content = it.message.substring(name.length + 4, it.message.length)
            if ("[$username]" == name.substring(0, name.length)) {
                name = "me"
            }
            RecyclerViewItemModel(name = name, content = content)
        } as MutableList<RecyclerViewItemModel>

        Log.d(ChatActivity.TAG, "Mapped to RV format")
        return messagesListInRVFormat
    }
}