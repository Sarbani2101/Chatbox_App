package com.example.chatbox_app.adapters

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chat_application.dataclass.User
import com.example.chatbox_app.R

class CallsAdapter(private val context: Context, private val userList: List<User>) :
    RecyclerView.Adapter<CallsAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val profileImage: ImageView = itemView.findViewById(R.id.userProfileImage)
        val userName: TextView = itemView.findViewById(R.id.callName)
        val callIcon: ImageView = itemView.findViewById(R.id.callIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.call_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = userList[position]
        holder.userName.text = user.name

        // Load profile image from Base64 or URL
        if (user.profileImage!!.startsWith("http")) {
            Glide.with(context).load(user.profileImage).into(holder.profileImage)
        } else {
            holder.profileImage.setImageBitmap(decodeBase64(user.profileImage))
        }

        // Call button action (future implementation)
        holder.callIcon.setOnClickListener {
            // Implement call functionality
        }
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    private fun decodeBase64(base64String: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
