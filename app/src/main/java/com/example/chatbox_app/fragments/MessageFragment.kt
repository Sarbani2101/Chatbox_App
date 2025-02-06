package com.example.chatbox_app.fragments

import ChatListAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.chatbox_app.activities.ChatActivity
import com.example.chatbox_app.adapter.StoriesAdapter
import com.example.chatbox_app.databinding.FragmentMessageBinding
import com.example.chatbox_app.dataclass.Chat
import com.example.chatbox_app.R
import com.example.chatbox_app.activities.ProfileActivity
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!

    private lateinit var storiesAdapter: StoriesAdapter
    private lateinit var chatListAdapter: ChatListAdapter
    private val userList = mutableListOf<User>()
    private val chatList = mutableListOf<Chat>()

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    private var usersLoaded = false
    private var chatsLoaded = false

    companion object {
        private const val MAX_RADIUS_KM = 90.0
        private const val CHAT_ACTIVITY_REQUEST_CODE = 101
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().reference

        binding.progressBar.visibility = View.VISIBLE
        fetchUsersFromDatabase()
        mAuth.currentUser?.uid?.let { fetchChatsFromDatabase(it) }

        setupRecyclerViews()
        setupClickListeners()
        setupSwipeToDelete()
    }

    private fun setupRecyclerViews() {
        val currentUserName = mAuth.currentUser?.displayName ?: "Guest"
        val currentUserId = mAuth.currentUser?.uid ?: ""

        storiesAdapter = StoriesAdapter(userList) { selectedUser ->
            navigateToChatActivity(currentUserName, currentUserId, selectedUser.uid, selectedUser.name)
        }
        binding.storiesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.storiesRecyclerView.adapter = storiesAdapter

        chatListAdapter = ChatListAdapter(chatList) { selectedChat ->
            navigateToChatActivity(selectedChat)
        }
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecyclerView.adapter = chatListAdapter
    }

    private fun navigateToChatActivity(currentUserName: String, currentUserId: String, selectedUserId: String, selectedUserName: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("user_name", mAuth.currentUser?.displayName)
            putExtra("uid", mAuth.currentUser?.uid)
            putExtra("selected_user_id", selectedUserId)
            putExtra("selected_user_name", selectedUserName)
        }
        startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
    }

    private fun navigateToChatActivity(selectedChat: Chat) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("user_name", mAuth.currentUser?.displayName)
            putExtra("uid", mAuth.currentUser?.uid)
            putExtra("selected_user_id", selectedChat.receiverUid)
            putExtra("selected_user_name", selectedChat.receiverName)
        }
        startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHAT_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val lastMessage = data?.getStringExtra("last_message") ?: ""
            val timestamp = data?.getLongExtra("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
            val receiverUid = data?.getStringExtra("receiver_uid") ?: ""
            val name = data?.getStringExtra("name") ?: "Unknown User"

            // Update the chat list with the new message
            updateChatList(receiverUid, name, lastMessage, timestamp)
        }
    }

    private fun updateChatList(receiverUid: String, name: String, lastMessage: String, timestamp: Long) {
        val chat = Chat(receiverName = name, receiverUid = receiverUid, lastMessage = lastMessage, timestamp = timestamp, isRead = false)

        // Save or update the chat in Firebase
        val chatRef = mDbRef.child("chats").child(mAuth.currentUser?.uid ?: "").child(receiverUid)
        chatRef.setValue(chat)

        // Update the chatList locally and refresh the adapter
        val existingChatIndex = chatList.indexOfFirst { it.receiverUid == receiverUid }
        if (existingChatIndex != -1) {
            chatList[existingChatIndex] = chat
        } else {
            chatList.add(0, chat)  // Always add the new message at the top
        }
        chatList.sortByDescending { it.timestamp }  // Sort chats by timestamp
        chatListAdapter.notifyDataSetChanged()  // Notify the adapter to refresh the list
    }


    private fun setupClickListeners() {
        binding.profileIcon.setOnClickListener {
            startActivity(Intent(context, ProfileActivity::class.java))
        }
        binding.searchIcon.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(android.R.id.content, SearchFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chat = chatList[position]

                if (direction == ItemTouchHelper.LEFT) {
                    // Swipe Left: Mark as Read/Unread
                    toggleReadStatus(chat)
                } else if (direction == ItemTouchHelper.RIGHT) {
                    // Swipe Right: Delete the chat
                    deleteChat(chat)
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val background = ColorDrawable(Color.TRANSPARENT)

                if (dX < 0) {
                    // Swipe left: Mark as Read/Unread
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    background.draw(c)

                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_unread)
                    val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                    val iconLeft = itemView.right - icon?.intrinsicWidth!! - 100
                    val iconRight = itemView.right - 100

                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon.draw(c)
                }

                if (dX > 0) {
                    // Swipe right: Show delete option
                    val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)
                    val iconMargin = (itemView.height - (icon?.intrinsicHeight ?: 0)) / 2
                    val iconTop = itemView.top + iconMargin
                    val iconBottom = iconTop + (icon?.intrinsicHeight ?: 0)
                    val iconLeft = itemView.left + 100
                    val iconRight = itemView.left + 200

                    icon?.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    icon?.draw(c)
                }
            }

            override fun onChildDrawOver(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                super.onChildDrawOver(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.chatListRecyclerView)
    }

    private fun toggleReadStatus(chat: Chat) {
        val updatedChat = chat.copy(isRead = !chat.isRead)

        // Update in Firebase
        mDbRef.child("chats").child(mAuth.currentUser?.uid ?: "").child(chat.receiverUid).setValue(updatedChat)

        // Update locally and refresh the adapter
        val index = chatList.indexOfFirst { it.receiverUid == chat.receiverUid }
        if (index != -1) {
            chatList[index] = updatedChat
            chatListAdapter.notifyItemChanged(index)
        }
    }

    private fun deleteChat(chat: Chat) {
        // Delete from Firebase
        mDbRef.child("chats").child(mAuth.currentUser?.uid ?: "").child(chat.receiverUid).removeValue()

        // Remove locally and refresh the adapter
        val index = chatList.indexOfFirst { it.receiverUid == chat.receiverUid }
        if (index != -1) {
            chatList.removeAt(index)
            chatListAdapter.notifyItemRemoved(index)
        }
    }

    private fun fetchUsersFromDatabase() {
        mDbRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                val currentUser = mAuth.currentUser ?: return
                val currentUserRef = snapshot.child(currentUser.uid)

                val currentLatitude = currentUserRef.child("latitude").getValue(Double::class.java) ?: 0.0
                val currentLongitude = currentUserRef.child("longitude").getValue(Double::class.java) ?: 0.0

                val nearbyUsers = snapshot.children.mapNotNull { data ->
                    val user = data.getValue(User::class.java)
                    user?.takeIf { it.uid != currentUser.uid && calculateDistance(currentLatitude, currentLongitude, it.latitude, it.longitude) <= MAX_RADIUS_KM }
                }

                userList.clear()
                userList.addAll(nearbyUsers)

                storiesAdapter.notifyDataSetChanged()
                usersLoaded = true
                checkDataLoadingComplete()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load users: ${error.message}", Toast.LENGTH_SHORT).show()
                usersLoaded = true
                checkDataLoadingComplete()
            }
        })
    }

    private fun fetchChatsFromDatabase(userId: String) {
        mDbRef.child("chats").child(userId).orderByChild("timestamp")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    chatList.clear()
                    for (data in snapshot.children) {
                        val chat = data.getValue(Chat::class.java)
                        chat?.let {
                            chatList.add(it)
                        }
                    }
                    chatList.sortByDescending { it.timestamp }
                    chatListAdapter.notifyDataSetChanged()
                    chatsLoaded = true
                    checkDataLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load chats: ${error.message}", Toast.LENGTH_SHORT).show()
                    chatsLoaded = true
                    checkDataLoadingComplete()
                }
            })

    }

    private fun checkDataLoadingComplete() {
        if (chatsLoaded && usersLoaded && binding.progressBar.visibility == View.VISIBLE) {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371  // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c  // Return distance in kilometers
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
