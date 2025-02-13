package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        groupAdapter = GroupAdapter(users) { user ->
            addUserToGroup(user)
        }

        binding.groupRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = groupAdapter
        }
        binding.adminName.text = "Admin: John"
        binding.groupRecyclerView.visibility = View.GONE
    }

    private fun addUserToGroup(user: User) {
        Toast.makeText(requireContext(), "${user.name} added to the group", Toast.LENGTH_SHORT).show()
        binding.groupRecyclerView.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
