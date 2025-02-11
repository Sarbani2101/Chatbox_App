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
    private val searchResults = mutableListOf<ChatItem>()

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("chats")

        // Setup RecyclerView
        searchAdapter = SearchAdapter(requireContext(), searchResults) { chatItem ->
            // Handle chat click event
        }
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewSearch.adapter = searchAdapter

        // Text change listener for searching
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            @SuppressLint("NotifyDataSetChanged")
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    binding.searchIcon.visibility = View.VISIBLE
                    searchChats(s.toString().trim())
                } else {
                    binding.searchIcon.visibility = View.GONE
                    searchResults.clear()
                    searchAdapter.notifyDataSetChanged()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.searchClearIcon.setOnClickListener {
            binding.searchEditText.text.clear()
            searchResults.clear()
            searchAdapter.notifyDataSetChanged()
        }

        return binding.root
    }

    // Search for names and sort alphabetically
    private fun searchChats(query: String) {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                searchResults.clear()

                for (chatSnapshot in snapshot.children) {
                    val chatItem = chatSnapshot.getValue(ChatItem::class.java)
                    if (chatItem != null && chatItem.username.contains(query, ignoreCase = true)) {
                        searchResults.add(chatItem)
                    }
                }

                // Sort results alphabetically by name
                searchResults.sortBy { it.username }

                searchAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load search results", Toast.LENGTH_SHORT).show()
            }
        })
    }

}
