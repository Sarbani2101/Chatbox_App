package com.example.chatbox_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbox_app.R
import com.example.chatbox_app.dataclass.ChatItem

class SearchAdapter(
    private val context: Context,
    private val chatItemList: MutableList<ChatItem>,
    private val onChatItemClick: (ChatItem) -> Unit // Callback for item clicks
) : RecyclerView.Adapter<SearchAdapter.SearchViewHolder>() {

    class SearchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.usernameTextView)
        val lastMessage: TextView = view.findViewById(R.id.lastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_search_result, parent, false)
        return SearchViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        val chatItem = chatItemList[position]
        holder.username.text = chatItem.username
        holder.lastMessage.text = chatItem.lastMessage

        // Handle item click
        holder.itemView.setOnClickListener {
            onChatItemClick(chatItem)
        }
    }

    override fun getItemCount(): Int {
        return chatItemList.size
    }
}