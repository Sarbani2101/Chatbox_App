package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.adapter.SearchAdapter
import com.example.chatbox_app.databinding.FragmentSearchBinding
import com.example.chatbox_app.dataclass.ChatItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var database: DatabaseReference
    private lateinit var searchAdapter: SearchAdapter
    private var searchResults: MutableList<ChatItem> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)

        // Initialize Firebase database reference
        database = FirebaseDatabase.getInstance().getReference("chats")

        // Set up the adapter for RecyclerView
        searchAdapter = SearchAdapter(searchResults)
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSearch.adapter = searchAdapter

        // Add TextWatcher to listen for search text changes
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    // Show the search icon
                    binding.searchIcon.visibility = View.VISIBLE
                    searchChats(s.toString())
                } else {
                    // Hide the search icon and clear results
                    binding.searchIcon.visibility = View.GONE
                    searchResults.clear()
                    searchAdapter.notifyDataSetChanged()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Set the listener for clearing the search text
        binding.searchClearIcon.setOnClickListener {
            binding.searchEditText.text.clear()
        }

        return binding.root
    }

    // Method to search chats based on query
    private fun searchChats(query: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                searchResults.clear()
                for (chatSnapshot in snapshot.children) {
                    val chatId = chatSnapshot.key ?: continue
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                    chatSnapshot.child("senderId").getValue(String::class.java) ?: ""
                    chatSnapshot.child("receiverId").getValue(String::class.java) ?: ""
                    val username = chatSnapshot.child("username").getValue(String::class.java) ?: ""

                    // Filter based on username or message
                    if (username.contains(query, true) || lastMessage.contains(query, true)) {
                        searchResults.add(ChatItem(chatId, username, lastMessage))
                    }
                }
                searchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load search results", Toast.LENGTH_SHORT).show()
            }
        })
    }
}