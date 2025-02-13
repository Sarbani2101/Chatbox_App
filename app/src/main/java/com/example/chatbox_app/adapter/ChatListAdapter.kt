package com.example.chatbox_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ChatListBinding
import com.example.chatbox_app.dataclass.Chat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(
    private val context: Context,
    private val chatList: MutableList<Chat>,
    private val onChatClick: (Chat) -> Unit
) : RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(private val binding: ChatListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            binding.chatUserName.text = chat.receiverName
            binding.chatMessagePreview.text = chat.lastMessage
            binding.chattime.text = formatTimestamp(chat.timestamp)

            // Load profile image using Glide
            Glide.with(binding.root.context)
                .load(chat.profileImageUrl)
                .placeholder(R.drawable.ic_default_profile_image)
                .into(binding.userProfileImage)

            // Show unread indicator based on chat status
            binding.unreadIndicator.visibility = if (!chat.isSent && !chat.isRead) {
                View.VISIBLE
            } else {
                View.GONE
            }

            // When the user clicks a chat, open the chat activity
            binding.root.setOnClickListener {
                onChatClick(chat)

                // If the message is received and unread, mark it as read
                if (!chat.isSent && !chat.isRead) {
                    markMessageAsRead(chat) // Mark message as read in the database
                }
            }

            listenForUserNameChanges(chat.receiverUid)
        }

        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(date)
        }
    }

    private fun markMessageAsRead(chat: Chat) {
        chat.isRead = true
        notifyItemChanged(chatList.indexOf(chat))

        // Update the read status in Firebase
        val chatRef = FirebaseDatabase.getInstance().getReference("chats").child(chat.receiverUid) // Adjust as needed
        chatRef.child("isRead").setValue(true).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(context, "Failed to update read status", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenForUserNameChanges(userId: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newName = snapshot.getValue(String::class.java)
                    if (!newName.isNullOrEmpty()) {
                        val chatIndex = chatList.indexOfFirst { it.receiverUid == userId }
                        if (chatIndex != -1) {
                            chatList[chatIndex].receiverName = newName
                            notifyItemChanged(chatIndex) // Notify adapter of the specific item change
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to update user name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}