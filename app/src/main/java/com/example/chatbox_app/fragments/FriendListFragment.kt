package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbox_app.R
import com.example.chatbox_app.adapter.AddFriendAdapter
import com.example.chatbox_app.adapter.FriendRequestAdapter
import com.example.chatbox_app.dataclass.FriendRequest
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class FriendListFragment : Fragment() {

    private lateinit var addFriendRecyclerView: RecyclerView
    private lateinit var friendRequestRecyclerView: RecyclerView
    private lateinit var addFriendAdapter: AddFriendAdapter
    private lateinit var friendRequestAdapter: FriendRequestAdapter
    private lateinit var userList: MutableList<User>
    private lateinit var friendRequests: ArrayList<FriendRequest>

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().getReference()

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_friend_list, container, false)

        addFriendRecyclerView = rootView.findViewById(R.id.addFriendRecyclerview)
        friendRequestRecyclerView = rootView.findViewById(R.id.callsRecyclerview)

        userList = mutableListOf()
        friendRequests = arrayListOf()

        loadUsers()
        loadFriendRequests()

        return rootView
    }

    private fun loadUsers() {
        // Get list of users (excluding current user)
        database.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (userSnapshot in snapshot.children) {
                    val user = userSnapshot.getValue(User::class.java)
                    if (user != null && user.uid != currentUserId) {
                        userList.add(user)
                    }
                }
                addFriendAdapter = AddFriendAdapter(requireContext(), userList)
                addFriendRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                addFriendRecyclerView.adapter = addFriendAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
    }

    private fun loadFriendRequests() {
        // Load friend requests for the current user
        currentUserId?.let {
            database.child("FriendRequests").child(it).addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    friendRequests.clear()
                    for (requestSnapshot in snapshot.children) {
                        val request = requestSnapshot.getValue(FriendRequest::class.java)
                        if (request != null && request.status == "pending") {
                            friendRequests.add(request)
                        }
                    }
                    friendRequestAdapter = FriendRequestAdapter(requireContext(), friendRequests)
                    friendRequestRecyclerView.layoutManager = LinearLayoutManager(requireContext())
                    friendRequestRecyclerView.adapter = friendRequestAdapter
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
        }
    }
}