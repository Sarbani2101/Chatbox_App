package com.example.chatbox_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.StoryListBinding
import com.example.chatbox_app.dataclass.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class StoriesAdapter(
    private val context: Context,
    private var userList: MutableList<User>,
    private val onClickListener: (User) -> Unit,
) : RecyclerView.Adapter<StoriesAdapter.StoriesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        val binding = StoryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    inner class StoriesViewHolder(private val binding: StoryListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.storyTitle.text = user.name.ifEmpty { "Unknown" }
            Glide.with(binding.root.context)
                .load(user.profileImage.takeIf { it.isNotEmpty() } ?: R.drawable.ic_default_profile_image)
                .placeholder(R.drawable.ic_default_profile_image)
                .error(R.drawable.ic_default_profile_image)
                .into(binding.storyImage)

            binding.root.setOnClickListener { onClickListener(user) }
            listenForUserNameChanges(user.uid)
        }
    }

    private fun listenForUserNameChanges(uid: String) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)

        userRef.child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newName = snapshot.getValue(String::class.java)
                    if (!newName.isNullOrEmpty()) {
                        // Update the user name in the userList
                        val userIndex = userList.indexOfFirst { it.uid == uid }
                        if (userIndex != -1) {
                            userList[userIndex].name = newName
                            notifyItemChanged(userIndex) // Notify adapter of the specific item change
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



