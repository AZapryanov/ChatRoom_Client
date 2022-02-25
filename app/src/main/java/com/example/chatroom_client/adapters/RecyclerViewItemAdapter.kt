package com.example.chatroom_client.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatroom_client.R
import com.example.chatroom_client.models.RecyclerViewItemModel


class RecyclerViewItemAdapter(private val items: MutableList<RecyclerViewItemModel>,
                              private val template: String?
) : RecyclerView.Adapter<RecyclerViewItemAdapter.RvEntryViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RvEntryViewHolder {
        var view: View? = null
        if (template == "my") {
            view = LayoutInflater.from(parent.context).inflate(R.layout.rv_my_messages_template, parent, false)
        } else if (template == "others") {
            view = LayoutInflater.from(parent.context).inflate(R.layout.rv_others_messages_template, parent, false)
        }
        return RvEntryViewHolder(view)
    }

    override fun onBindViewHolder(holder: RvEntryViewHolder, position: Int) {
        holder.bindView(items[position])
    }

    override fun getItemCount(): Int {
        return items.size
    }

    class RvEntryViewHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!) {
        fun bindView(item: RecyclerViewItemModel) {
            val rvEntryBody = itemView.findViewById<TextView>(R.id.tvEntryBody)
            rvEntryBody.text = item.body
        }
    }
}