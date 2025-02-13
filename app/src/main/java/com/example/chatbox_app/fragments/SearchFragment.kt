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
import com.example.chatbox_app.dataclass.User
import com.google.firebase.database.*

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var database: DatabaseReference
    private lateinit var searchAdapter: SearchAdapter
    private val chatList = mutableListOf<User>() // Full user list from Firebase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("users") // Reference to user data

        // Initialize RecyclerView
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())
        searchAdapter = SearchAdapter(requireContext(), mutableListOf()) { chatItem ->
            // Handle item click (e.g., navigate to chat or show user details)
            Toast.makeText(requireContext(), "Clicked on ${chatItem.name}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewSearch.adapter = searchAdapter

        // Fetch all users initially
        fetchUsers()

        // Handle search input text change
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                filterUsers(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchClearIcon.setOnClickListener {
            binding.searchEditText.text.clear()
            fetchUsers()
        }

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

    private fun fetchUsers() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    user?.let { chatList.add(it) }
                }
                searchAdapter.updateList(chatList) // Update adapter with full user list
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun filterUsers(query: String) {
        val filteredList = chatList.filter { it.name.contains(query, ignoreCase = true) }
        searchAdapter.updateList(filteredList)

        if (filteredList.isEmpty() && query.isNotEmpty()) {
            Toast.makeText(requireContext(), "No users found for \"$query\"", Toast.LENGTH_SHORT).show()
        }
    }
}
