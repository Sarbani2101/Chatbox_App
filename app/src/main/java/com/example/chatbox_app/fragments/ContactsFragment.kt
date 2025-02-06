package com.example.chatbox_app.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.adapter.ContactsAdapter
import com.example.chatbox_app.databinding.FragmentContactsBinding
import com.example.chatbox_app.dataclass.Contact
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ContactsFragment : Fragment() {

    private lateinit var binding: FragmentContactsBinding
    private lateinit var contactsAdapter: ContactsAdapter

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private val contactList = mutableListOf<Contact>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentContactsBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users") // Ensure this matches your Firebase database structure

        // Setup RecyclerView
        binding.contactRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        contactsAdapter = ContactsAdapter(contactList) { contact ->
            // Handle contact click (if needed)
            onContactClick(contact)
        }
        binding.contactRecyclerview.adapter = contactsAdapter

        // Fetch contacts from Firebase
        fetchContacts()

        return binding.root
    }

    private fun fetchContacts() {
        val currentUserId = auth.currentUser?.uid ?: return

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                contactList.clear()
                if (snapshot.exists()) {
                    for (userSnapshot in snapshot.children) {
                        val user = userSnapshot.getValue(User::class.java)
                        if (user != null && user.uid != currentUserId) { // Exclude current user
                            val contact = Contact(
                                name = user.name ?: "Unknown",
                                status = "Available", // Example status (can be dynamic)
                                profileImage = user.profileImage ?: "" // Handle null case
                            )
                            contactList.add(contact)
                        }
                    }
                    if (contactList.isEmpty()) {
                        Log.d("ContactsFragment", "No contacts found.")
                    }
                    // Sort contacts alphabetically by name
                    contactList.sortBy { it.name }
                    // Notify the adapter to update RecyclerView
                    contactsAdapter.notifyDataSetChanged()
                } else {
                    Log.d("ContactsFragment", "No users found in the database.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ContactsFragment", "Error fetching users: ${error.message}")
            }
        })
    }

    private fun onContactClick(contact: Contact) {
        // Handle the contact item click (e.g., navigate to ChatActivity)
        Log.d("ContactsFragment", "Clicked on contact: ${contact.name}")
        // Example: Navigate to ChatActivity
        // val intent = Intent(requireContext(), ChatActivity::class.java)
        // intent.putExtra("contact_name", contact.name)
        // intent.putExtra("contact_image", contact.profileImage)
        // startActivity(intent)
    }
}