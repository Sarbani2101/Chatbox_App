package com.example.chatbox_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.GroupListBinding
import com.example.chatbox_app.dataclass.User

class GroupAdapter(
    private val users: List<User>,
    private val onAddGroupClick: (User) -> Unit
) : RecyclerView.Adapter<GroupAdapter.GroupViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val binding = GroupListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return GroupViewHolder(binding)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
    }

    override fun getItemCount(): Int = users.size

    inner class GroupViewHolder(private val binding: GroupListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            Glide.with(itemView.context)
                .load(user.profileImage)
                .placeholder(R.drawable.ic_default_profile_image)
                .into(binding.userProfileImage)// Set profile image

            binding.addGroup.setOnClickListener {
                onAddGroupClick(user) // Handle the click for adding users to the group
            }
        }
    }
}
