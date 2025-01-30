package com.example.chatbox_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chat_application.dataclass.User
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.StoryListBinding

class StoriesAdapter(
    private val userList: List<User>,
    private val onUserClick: (User) -> Unit
) : RecyclerView.Adapter<StoriesAdapter.StoriesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
        val binding = StoryListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StoriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {
        val user = userList[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = userList.size

    inner class StoriesViewHolder(private val binding: StoryListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.storyTitle.text = user.name ?: "Unknown"

            val profileImage = user.profileImage
            if (!profileImage.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(profileImage)
                    .placeholder(R.drawable.ic_default_profile_image)
                    .error(R.drawable.ic_default_profile_image)
                    .into(binding.storyImage)
            }

            binding.root.setOnClickListener {
                onUserClick(user)
            }
        }
    }
}
