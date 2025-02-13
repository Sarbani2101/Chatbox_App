package com.example.chatbox_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.StoryListBinding
import com.example.chatbox_app.dataclass.User

class StoriesAdapter(
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
        }
    }
}



