package com.example.chatbox_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.dataclass.Contact
import de.hdodenhof.circleimageview.CircleImageView

class ContactsAdapter(
    private val contacts: List<Contact>,
    private val onContactClick: (Contact) -> Unit // Callback for contact click
) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.contacts_list, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(contact)
    }

    override fun getItemCount(): Int = contacts.size

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.userProfileImage)
        private val contactName: TextView = itemView.findViewById(R.id.adminName)

        fun bind(contact: Contact) {
            contactName.text = contact.name

            // Use Glide to load the profile image
            if (contact.profileImage.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(contact.profileImage)
                    .placeholder(R.drawable.ic_default_profile_image) // Default image
                    .into(profileImage)
            } else {
                // Set default image if profileImage is empty
                profileImage.setImageResource(R.drawable.ic_default_profile_image)
            }

            // Set click listener for the contact item
            itemView.setOnClickListener {
                onContactClick(contact)
            }
        }
    }
}