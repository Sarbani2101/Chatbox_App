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
import com.example.chatbox_app.adapters.CallsAdapter
import com.example.chatbox_app.dataclass.User
import com.google.firebase.database.*

class CallsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var callsAdapter: CallsAdapter
    private lateinit var userList: ArrayList<User>
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calls, container, false)

        recyclerView = view.findViewById(R.id.callsRecyclerview)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        userList = ArrayList()
        callsAdapter = CallsAdapter(requireContext(), userList)
        recyclerView.adapter = callsAdapter

        database = FirebaseDatabase.getInstance().getReference("Users")

        fetchUsers()

        return view
    }

    private fun fetchUsers() {
        database.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(User::class.java)
                    if (user != null) {
                        userList.add(user)
                    }
                }
                callsAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }
}
