package com.example.chatroom_client.view_model

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.chatroom_client.models.RecyclerViewItemModel
import com.example.chatroom_client.ui.ChatActivity
import src.main.graphql.MessageListQuery

class ChatActivityViewModel: ViewModel() {
    var mRecyclerViewList: MutableList<RecyclerViewItemModel> = mutableListOf()

    private var mUserId: Int? = null
    private var mUsername: String? = null

    fun addItemToList(itemToAdd: RecyclerViewItemModel) {
        mRecyclerViewList.add(itemToAdd)
        Log.d(ChatActivity.TAG, "Message added to RV list")
    }

    fun addEntireList(list: MutableList<RecyclerViewItemModel>) {
        mRecyclerViewList = list
        Log.d(ChatActivity.TAG, "Entire list added")
    }

    fun mapToRecyclerViewFormat(
        rawMessages: List<MessageListQuery.GetAllMessage>?,
        username: String?
    ): MutableList<RecyclerViewItemModel> {
        val messagesListInRVFormat = rawMessages?.map {
            var name = it.message.substring(0, it.message.indexOf(END_OF_USERNAME_KEY_CHAR))
            val content = it.message.substring(name.length + 2, it.message.length)
            if ("[$username]" == name.substring(0, name.length)) {
                name = "me"
            }
            RecyclerViewItemModel(name = name, content = content)
        } as MutableList<RecyclerViewItemModel>

        Log.d(ChatActivity.TAG, "Mapped to RV format")
        return messagesListInRVFormat
    }

    fun setUsernameValue(username: String?) {
        this.mUsername = username
    }

    fun setUserIdValue(userId: Int?) {
        this.mUserId = userId
    }

    fun getUsernameValue() : String? {
        return mUsername
    }

    fun getUserIdValue() : Int? {
        return mUserId
    }

    companion object {
        private const val END_OF_USERNAME_KEY_CHAR = ':'
    }
}