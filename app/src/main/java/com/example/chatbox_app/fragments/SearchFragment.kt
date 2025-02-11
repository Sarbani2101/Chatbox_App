package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.adapter.SearchAdapter
import com.example.chatbox_app.databinding.FragmentSearchBinding
import com.example.chatbox_app.dataclass.ChatItem
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var database: DatabaseReference
    private lateinit var searchAdapter: SearchAdapter
    private val chatList = mutableListOf<ChatItem>() // Full user list from Firebase
    private val filteredList = mutableListOf<ChatItem>() // Filtered user list based on search query

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("users") // Reference to user data

        // Initialize RecyclerView
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter(requireContext(), filteredList) { chatItem ->
            // Handle item click (e.g., navigate to chat or show user details)
            Toast.makeText(requireContext(), "Clicked on ${chatItem.username}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewSearch.adapter = searchAdapter

        // Fetch all users initially
        fetchUsers()

        // Handle search input text change
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Perform filtering as the user types
                val query = s.toString().trim()
                filterUsers(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Handle search icon click
        binding.searchIcon.setOnClickListener {
            val query = binding.searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                filterUsers(query)
            } else {
                Toast.makeText(requireContext(), "Enter a name to search", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    // Fetch all users from Firebase
    private fun fetchUsers() {
        database.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(ChatItem::class.java)
                    user?.let { chatList.add(it) }
                }
                // Initially, show all users
                filteredList.clear()
                filteredList.addAll(chatList)
                searchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Filter users based on search input
    @SuppressLint("NotifyDataSetChanged")
    private fun filterUsers(query: String) {
        filteredList.clear()

        // Perform filtering (case-insensitive)
        filteredList.addAll(chatList.filter { it.username.contains(query, ignoreCase = true) })

        // Update the RecyclerView with the filtered list
        searchAdapter.notifyDataSetChanged()

        // Show a message if no users match the search query
        if (filteredList.isEmpty()) {
            Toast.makeText(requireContext(), "No users found for \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }
}