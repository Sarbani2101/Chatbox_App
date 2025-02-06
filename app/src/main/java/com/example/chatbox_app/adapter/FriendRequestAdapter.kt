package com.example.chatbox_app.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbox_app.R
import com.example.chatbox_app.dataclass.FriendRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class FriendRequestAdapter(
    private val context: Context,
    private val requestList: ArrayList<FriendRequest>
) : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val profileImage: ImageView = view.findViewById(R.id.profileIcon)
        val username: TextView = view.findViewById(R.id.usernameTextView)
        val acceptButton: Button = view.findViewById(R.id.acceptButton)
        val rejectButton: Button = view.findViewById(R.id.rejectButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val request = requestList[position]

        // Set username
        holder.username.text = request.username

        // Decode and set profile image (if stored as Base64)
        if (request.profileImage.isNotEmpty()) {
            val decodedBytes = Base64.decode(request.profileImage, Base64.DEFAULT)
            holder.profileImage.setImageBitmap(BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size))
        }

        // Accept Friend Request
        holder.acceptButton.setOnClickListener {
            acceptFriendRequest(request.senderId)
            requestList.removeAt(position)
            notifyDataSetChanged()
        }

        // Reject Friend Request
        holder.rejectButton.setOnClickListener {
            rejectFriendRequest(request.senderId)
            requestList.removeAt(position)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    private fun acceptFriendRequest(senderId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference()

        // Add to Friends List
        database.child("Friends").child(currentUserId).child(senderId).setValue(true)
        database.child("Friends").child(senderId).child(currentUserId).setValue(true)

        // Remove from Friend Requests
        database.child("FriendRequests").child(currentUserId).child(senderId).removeValue()
    }

    private fun rejectFriendRequest(senderId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance().getReference()

        // Remove from Friend Requests
        database.child("FriendRequests").child(currentUserId).child(senderId).removeValue()
    }
}