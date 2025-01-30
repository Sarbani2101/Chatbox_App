package com.example.chatbox_app.fragments

import Chat
import ChatListAdapter
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.activities.ChatActivity
import com.example.chatbox_app.adapter.StoriesAdapter
import com.example.chatbox_app.databinding.FragmentMessageBinding
import com.example.chat_application.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream

class MessageFragment : Fragment() {

    private lateinit var binding: FragmentMessageBinding
    private lateinit var storiesAdapter: StoriesAdapter
    private lateinit var chatListAdapter: ChatListAdapter
    private var userList = mutableListOf<User>()
    private var chatList = mutableListOf<Chat>()
    private var imageUri: Uri? = null

    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val CHAT_ACTIVITY_REQUEST_CODE = 100
    }

    private var usersLoaded = false
    private var chatsLoaded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("user")

        // Show ProgressBar initially
        binding.progrssBar.visibility = View.VISIBLE

        // Fetch Data
        fetchUsersFromDatabase()
        loadProfileImage()
        val userId = mAuth.currentUser?.uid
        userId?.let { fetchChatsFromDatabase(it) }

        // Setup Recycler Views
        setupRecyclerViews()

        // Profile Image Change Listener
        binding.profileIcon.setOnClickListener {
            openImagePicker()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    private fun loadProfileImage() {
        val currentUserId = mAuth.currentUser?.uid ?: return
        mDbRef.child(currentUserId).child("profileImage").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val profileImageBase64 = snapshot.getValue(String::class.java)
                if (!profileImageBase64.isNullOrEmpty()) {
                    // Decode Base64 string to Bitmap
                    val decodedBytes = Base64.decode(profileImageBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    binding.profileIcon.setImageBitmap(bitmap) // Set the profile image
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load profile image.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadProfileImage(bitmap: Bitmap) {
        val currentUserId = mAuth.currentUser?.uid ?: return

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        mDbRef.child(currentUserId).child("profileImage").setValue(base64Image)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Profile image updated.", Toast.LENGTH_SHORT).show()
                    loadProfileImage()  // Reload the updated profile image
                } else {
                    Toast.makeText(context, "Failed to update profile image.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {
            when (requestCode) {
                CHAT_ACTIVITY_REQUEST_CODE -> {
                    val userName = data.getStringExtra("name")
                    val lastMessage = data.getStringExtra("last_message")
                    val timestamp = data.getStringExtra("timestamp")?.toLongOrNull() ?: System.currentTimeMillis()
                    val receiverUid = data.getStringExtra("receiver_uid")

                    // Find the existing chat or create a new one
                    val existingChat = chatList.find { it.receiverUid == receiverUid }
                    if (existingChat != null) {
                        // Update the existing chat
                        existingChat.lastMessage = lastMessage ?: "No message"
                        existingChat.timestamp = timestamp
                    } else {
                        // Add a new chat if it doesn't exist
                        val newChat = Chat(
                            receiverName = userName ?: "Unknown User",
                            receiverUid = receiverUid ?: "",
                            lastMessage = lastMessage ?: "No message",
                            timestamp = timestamp
                        )
                        chatList.add(newChat)
                    }

                    // Sort and refresh the chat list
                    chatList.sortByDescending { it.timestamp }
                    chatListAdapter.notifyDataSetChanged()
                }

                PICK_IMAGE_REQUEST -> {
                    imageUri = data.data
                    try {
                        val bitmap = MediaStore.Images.Media.getBitmap(context?.contentResolver, imageUri)
                        binding.profileIcon.setImageBitmap(bitmap)
                        bitmap?.let { uploadProfileImage(it) } // Upload the image
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error picking image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerViews() {
        val currentUserName = mAuth.currentUser?.displayName ?: "Guest"
        val currentUserId = mAuth.currentUser?.uid ?: ""

        // Setup Stories RecyclerView
        storiesAdapter = StoriesAdapter(userList) { selectedUser ->
            navigateToChatActivity(currentUserName, currentUserId, selectedUser)
        }
        binding.storiesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = storiesAdapter
        }

        // Setup Chat RecyclerView
        chatListAdapter = ChatListAdapter(chatList) { selectedChat ->
            navigateToChatActivity(
                currentUserName = mAuth.currentUser?.displayName ?: "Guest",
                currentUserId = mAuth.currentUser?.uid ?: "",
                receiverId = selectedChat.receiverUid,
                receiverName = selectedChat.receiverName
            )
        }
        binding.chatListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = chatListAdapter
        }
    }

    private fun navigateToChatActivity(userName: String, userId: String, selectedUser: User) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("user_name", userName)
            putExtra("uid", userId)
            putExtra("selected_user_id", selectedUser.uid)
            putExtra("selected_user_name", selectedUser.name)
        }
        startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
    }

    private fun navigateToChatActivity(currentUserName: String, currentUserId: String, receiverId: String, receiverName: String) {
        val intent = Intent(requireContext(), ChatActivity::class.java).apply {
            putExtra("user_name", currentUserName)
            putExtra("uid", currentUserId)
            putExtra("selected_user_id", receiverId)
            putExtra("selected_user_name", receiverName)
        }
        startActivityForResult(intent, CHAT_ACTIVITY_REQUEST_CODE)
    }

    private fun fetchUsersFromDatabase() {
        mDbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (data in snapshot.children) {
                    val user = data.getValue(User::class.java)
                    if (user != null && user.uid != mAuth.currentUser?.uid) {
                        userList.add(user)
                    }
                }
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
        mDbRef.child("chats").child(userId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                chatList.clear()
                for (data in snapshot.children) {
                    val chat = data.getValue(Chat::class.java)
                    if (chat != null) {
                        chatList.add(chat)
                    }
                }

                // Sort chats by timestamp in descending order (latest chats first)
                chatList.sortByDescending { it.timestamp }

                // Notify the adapter to update the RecyclerView
                chatListAdapter.notifyDataSetChanged()

                // Hide the ProgressBar
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
        if (usersLoaded && chatsLoaded) {
            binding.progrssBar.visibility = View.GONE // Hide ProgressBar
        }
    }
}
