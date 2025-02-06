package com.example.chatbox_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbox_app.R
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddFriendAdapter(
    private val context: Context,
    private val userList: List<User>
) : RecyclerView.Adapter<AddFriendAdapter.AddFriendViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_add_friend, parent, false)
        return AddFriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.name

        holder.btnAdd.setOnClickListener {
            sendFriendRequest(user.uid)
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    private fun sendFriendRequest(userId: String) {
        val requestRef = FirebaseDatabase.getInstance().getReference("FriendRequests")
        currentUserId?.let {
            val requestMap = mapOf(
                "sender" to it,
                "receiver" to userId,
                "status" to "pending"
            )

            // Store the request in Firebase
            requestRef.child(it).child(userId).setValue(requestMap)
                .addOnCompleteListener { task ->
                    // Successfully sent the request
                    when {
                        !task.isSuccessful -> {
                        }
                    }
                }
        }
    }

    class AddFriendViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userName: TextView = view.findViewById(R.id.storyTitle)
        val btnAdd: Button = view.findViewById(R.id.btnAdd)
    }
}