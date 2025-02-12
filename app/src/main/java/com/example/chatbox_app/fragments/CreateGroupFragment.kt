package com.example.chatbox_app.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.R
import com.example.chatbox_app.adapter.GroupAdapter
import com.example.chatbox_app.databinding.FragmentCreateGroupBinding
import com.example.chatbox_app.dataclass.User

class CreateGroupFragment : Fragment() {
    private var _binding: FragmentCreateGroupBinding? = null
    private val binding get() = _binding!!

    private lateinit var groupAdapter: GroupAdapter
    private val users = mutableListOf<User>() // This would come from your data source or API


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize the group adapter and set the RecyclerView
        groupAdapter = GroupAdapter(users) { user ->
            addUserToGroup(user)
        }

        binding.groupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }

        // Set admin name (who is creating the group)
        binding.adminName.text = "Admin: John" // Replace with the actual user's name

        // Hide RecyclerView until users are added
        binding.groupRecyclerView.visibility = View.GONE
    }

    private fun addUserToGroup(user: User) {
        // Add the user to the group
        // In this example, we're just displaying a message. In a real app, you'd update the group data.
        Toast.makeText(requireContext(), "${user.name} added to the group", Toast.LENGTH_SHORT).show()

        // After adding the user, hide the add button and show the group chat UI
        binding.groupRecyclerView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
