package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
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
import com.example.chatbox_app.R
import com.example.chatbox_app.activities.ChatActivity
import com.example.chatbox_app.adapter.StoriesAdapter
import com.example.chatbox_app.databinding.FragmentMessageBinding
import com.example.chatbox_app.dataclass.Chat
import com.example.chatbox_app.activities.ProfileActivity
import com.example.chatbox_app.adapter.ChatListAdapter
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("DEPRECATION")
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
        listenForNewMessages()
    }

    private fun setupRecyclerViews() {
        storiesAdapter = StoriesAdapter(userList) { selectedUser ->
            navigateToChatActivity(selectedUser.uid, selectedUser.name)
        }
        binding.storiesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.storiesRecyclerView.adapter = storiesAdapter

        chatListAdapter = ChatListAdapter(requireContext(),chatList) { selectedChat ->
            navigateToChatActivity(selectedChat)
            binding.txtNomsg.visibility = View.VISIBLE
        }
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.chatListRecyclerView.adapter = chatListAdapter

        enableSwipeToDelete()
    }

    private fun enableSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chatToDelete = chatList[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("Delete Chat")
                    .setMessage("Are you sure you want to delete ?")
                    .setPositiveButton("Yes") { _, _ ->
                        deleteChatFromFirebase(chatToDelete, position)
                    }
                    .setNegativeButton("No") { dialog, _ ->
                        chatListAdapter.notifyItemChanged(position)
                        dialog.dismiss()
                    }
                    .show()
            }

            override fun onChildDraw(

                c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder,
                dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_trash)!!

                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight

                val background = ColorDrawable(Color.RED)
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.draw(c)

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.chatListRecyclerView)
    }

    private fun deleteChatFromFirebase(chat: Chat, position: Int) {
        val currentUser = mAuth.currentUser?.uid ?: return

        // Check if the position is valid before proceeding
        if (position < 0 || position >= chatList.size) {
            Toast.makeText(context, "Invalid chat index.", Toast.LENGTH_SHORT).show()
            return
        }

        val senderChatRef = mDbRef.child("chats").child(currentUser).child(chat.receiverUid)
        val receiverChatRef = mDbRef.child("chats").child(chat.receiverUid).child(currentUser)

        senderChatRef.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                receiverChatRef.removeValue().addOnCompleteListener { task2 ->
                    if (task2.isSuccessful) {
                        if (chatList.contains(chat)) {
                            chatList.removeAt(position)
                            chatListAdapter.notifyItemRemoved(position)
                            binding.txtNomsg.visibility = if (chatList.isEmpty()) View.VISIBLE else View.GONE
                        }
                        else {
                            Toast.makeText(context, "Deleted.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Failed to delete chat for the receiver.", Toast.LENGTH_SHORT).show()
                        chatListAdapter.notifyItemChanged(position)
                    }
                }
            } else {
                Toast.makeText(context, "Failed to delete chat.", Toast.LENGTH_SHORT).show()
                chatListAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun navigateToChatActivity(selectedUserId: String, selectedUserName: String) {
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CHAT_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val lastMessage = data?.getStringExtra("last_message") ?: ""
            val timestamp = data?.getLongExtra("timestamp", System.currentTimeMillis()) ?: System.currentTimeMillis()
            val receiverUid = data?.getStringExtra("receiver_uid") ?: ""
            val receiverName = data?.getStringExtra("name") ?: "Unknown"

            // Update the chat list with the new message
            updateChatList(receiverUid, receiverName, lastMessage, timestamp)
            binding.txtNomsg.visibility = View.GONE
        } else {
            binding.txtNomsg.visibility = View.VISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateChatList(receiverUid: String, receiverName: String, lastMessage: String, timestamp: Long) {
        val senderUid = mAuth.currentUser?.uid ?: return
        val senderName = mAuth.currentUser?.displayName ?: "Unknown"

        // Firebase references for both the sender's and receiver's chat records
        val senderChatRef = mDbRef.child("chats").child(senderUid).child(receiverUid)
        val receiverChatRef = mDbRef.child("chats").child(receiverUid).child(senderUid)

        // For the current user (sender): mark the chat as sent and already read.
        val senderChat = Chat(
            receiverName = receiverName,
            receiverUid = receiverUid,
            lastMessage = lastMessage,
            timestamp = timestamp,
            isRead = true,    // Sender’s own message is considered read
            isSent = true,    // Message is sent by current user
            profileImageUrl = ""  // Provide profile image URL if available
        )

        // For the other user (receiver): mark the chat as received and unread.
        val receiverChat = Chat(
            receiverName = senderName,
            receiverUid = senderUid,
            lastMessage = lastMessage,
            timestamp = timestamp,
            isRead = false,   // New message is unread for receiver
            isSent = false,   // Message is received (not sent by them)
            profileImageUrl = ""  // Provide profile image URL if available
        )

        // Save both chat records in Firebase
        senderChatRef.setValue(senderChat)
        receiverChatRef.setValue(receiverChat)

        // Update the local chat list (for the sender) accordingly.
        val existingChatIndex = chatList.indexOfFirst { it.receiverUid == receiverUid }
        if (existingChatIndex != -1) {
            chatList[existingChatIndex] = senderChat
        } else {
            chatList.add(0, senderChat)
        }

        chatList.sortByDescending { it.timestamp }
        chatListAdapter.notifyDataSetChanged()
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

    private fun fetchUsersFromDatabase() {
        mDbRef.child("users").addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return // Prevent updates if fragment is destroyed

                userList.clear()
                val currentUser = mAuth.currentUser ?: return
                val currentUserRef = snapshot.child(currentUser.uid)

                val currentLatitude = currentUserRef.child("latitude").getValue(Double::class.java) ?: 0.0
                val currentLongitude = currentUserRef.child("longitude").getValue(Double::class.java) ?: 0.0

                val nearbyUsers = snapshot.children.mapNotNull { data ->
                    val user = data.getValue(User::class.java)
                    user?.takeIf {
                        it.uid != currentUser.uid &&
                                calculateDistance(currentLatitude, currentLongitude, it.latitude, it.longitude) <= MAX_RADIUS_KM
                    }
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
            .addValueEventListener(object : ValueEventListener {
                @SuppressLint("NotifyDataSetChanged")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!isAdded || _binding == null) return

                    chatList.clear()
                    val chatMap = mutableMapOf<String, Chat>()

                    for (data in snapshot.children) {
                        val chat = data.getValue(Chat::class.java)
                        chat?.let {
                            chatMap[it.receiverUid] = it
                            // Fetch user name if it's unknown
                            if (it.receiverName == "Unknown" || it.receiverName.isEmpty()) {
                                mDbRef.child("users").child(it.receiverUid)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(userSnapshot: DataSnapshot) {
                                            if (!isAdded || _binding == null) return

                                            val actualName = userSnapshot.child("name").getValue(String::class.java) ?: "Unknown"
                                            it.receiverName = actualName
                                            // Update the chat list UI with the new name
                                            updateChatListUI(chatMap)
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(context, "Failed to fetch user details: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                        }
                    }
                    updateChatListUI(chatMap)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(context, "Failed to load chats: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateChatListUI(chatMap: Map<String, Chat>) {
        if (!isAdded || _binding == null) return

        chatList.clear()
        chatList.addAll(chatMap.values)
        chatList.sortByDescending { it.timestamp }
        chatListAdapter.notifyDataSetChanged()

        if (chatList.isEmpty()) {
            binding.txtNomsg.visibility = View.VISIBLE
        } else {
            binding.txtNomsg.visibility = View.GONE
        }

        chatsLoaded = true
        checkDataLoadingComplete()
    }

    private fun checkDataLoadingComplete() {
        if (chatsLoaded && usersLoaded && binding.progressBar.visibility == View.VISIBLE) {
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371  // Earth radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun listenForNewMessages() {
        val currentUserId = mAuth.currentUser?.uid ?: return
        mDbRef.child("chats").child(currentUserId).addChildEventListener(object : ChildEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chat = snapshot.getValue(Chat::class.java)
                chat?.let {
                    // Check if the chat already exists in the list
                    val existingChatIndex = chatList.indexOfFirst { it.receiverUid == chat.receiverUid }
                    if (existingChatIndex != -1) {
                        // Update existing chat
                        chatList[existingChatIndex] = chat
                    } else {
                        // Add new chat to the list
                        chatList.add(chat)
                    }
                    chatList.sortByDescending { it.timestamp } // Sort chats by timestamp
                    chatListAdapter.notifyDataSetChanged() // Notify adapter of changes
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                // Handle chat updates if needed
                val updatedChat = snapshot.getValue(Chat::class.java)
                updatedChat?.let {
                    // Update the existing chat in the list
                    val existingChatIndex = chatList.indexOfFirst { it.receiverUid == updatedChat.receiverUid }
                    if (existingChatIndex != -1) {
                        chatList[existingChatIndex] = updatedChat
                        chatListAdapter.notifyItemChanged(existingChatIndex) // Notify adapter of the update
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Handle chat removal if needed
                val removedChat = snapshot.getValue(Chat::class.java)
                removedChat?.let {
                    // Remove the chat from the list
                    val existingChatIndex = chatList.indexOfFirst { it.receiverUid == removedChat.receiverUid }
                    if (existingChatIndex != -1) {
                        chatList.removeAt(existingChatIndex)
                        chatListAdapter.notifyItemRemoved(existingChatIndex) // Notify adapter of the removal
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load new messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}