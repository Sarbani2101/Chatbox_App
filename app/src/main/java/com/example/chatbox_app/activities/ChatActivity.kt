package com.example.chatbox_app.activities

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.chatbox_app.adapter.ChatAdapter
import com.example.chatbox_app.databinding.ActivityChatBinding
import com.example.chatbox_app.dataclass.Chat
import com.example.chatbox_app.dataclass.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Suppress("SameParameterValue")
class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<Message>()

    private lateinit var mDbRef: DatabaseReference

    private var currentUserId: String? = null
    private var selectedUserId: String? = null
    private var chatId: String? = null
    private var receiverName: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("users") // For user data
        database = FirebaseDatabase.getInstance().reference  // For chats
        currentUserId = mAuth.currentUser?.uid
        selectedUserId = intent.getStringExtra("selected_user_id")
        receiverName = intent.getStringExtra("selected_user_name")

        // Generate a unique chat ID based on the user ids
        chatId = generateChatId()
        if (chatId == null) {
            Toast.makeText(this, "Chat initialization failed", Toast.LENGTH_SHORT).show()
            return
        }

        // Set up chat header details
        binding.userName.text = receiverName ?: "Unknown User"
        binding.userStatus.text = "Active now"

        setupRecyclerView()
        loadMessages()
        checkUserStatus()
        listenForUserNameChanges()

        // Back button logic
        binding.backImg.setOnClickListener {
            sendBackChatDataToMainActivity()
            finish()
        }

        setupMessageInput()

        // Handle send button click
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(messageText)
                binding.messageEditText.text.clear()
                updateTypingStatus(false) // Reset typing status after sending message
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateUserStatus("Active now")
    }

    override fun onPause() {
        super.onPause()
        updateLastSeen()
    }

    private fun generateChatId(): String? {
        if (currentUserId != null && selectedUserId != null) {
            return if (currentUserId!! < selectedUserId!!) {
                "$currentUserId-$selectedUserId"
            } else {
                "$selectedUserId-$currentUserId"
            }
        }
        return null
    }

    private fun setupRecyclerView() {
        chatAdapter = ChatAdapter(messages, currentUserId ?: "")
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply { stackFromEnd = true }
            adapter = chatAdapter
        }
    }

    private fun loadMessages() {
        if (chatId == null) return

        database.child("chats").child(chatId!!).addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                message?.let {
                    messages.add(it)
                    chatAdapter.notifyItemInserted(messages.size - 1)
                    binding.chatRecyclerView.scrollToPosition(messages.size - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to load messages: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupMessageInput() {
        binding.messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrBlank()) {
                    binding.sendButton.visibility = View.GONE
                    binding.microImg.visibility = View.VISIBLE
                    updateTypingStatus(false)
                } else {
                    binding.sendButton.visibility = View.VISIBLE
                    binding.microImg.visibility = View.GONE
                    updateTypingStatus(true)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun sendMessage(message: String) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val selectedUserId = intent.getStringExtra("selected_user_id") ?: return
        val selectedUserName = intent.getStringExtra("selected_user_name") ?: return

        // Create a Message object
        val chatMessage = Message(
            senderId = currentUserId,
            receiverId = selectedUserId,
            message = message,
            timestamp = System.currentTimeMillis().toString(),
            senderName = mAuth.currentUser?.displayName ?: "Unknown" // Store sender's name
        )

        // Save the chat message to Firebase under the chat ID
        database.child("chats").child(chatId!!).push().setValue(chatMessage)
        updateTypingStatus(false)

        // Save chat summary
        val chatSummary = Chat(
            receiverName = selectedUserName,
            receiverUid = selectedUserId,
            lastMessage = message,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        mDbRef.child("chats").child(currentUserId).child(selectedUserId).setValue(chatSummary)

        // Return updated data back (if needed)
        val resultIntent = Intent().apply {
            putExtra("last_message", message)
            putExtra("timestamp", System.currentTimeMillis())
            putExtra("receiver_uid", selectedUserId)
            putExtra("name", selectedUserName)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        updateUserStatus("Active now")
    }

    private fun updateUserStatus(status: String) {
        mDbRef.child(currentUserId!!).child("status").setValue(status)
    }

    private fun updateLastSeen() {
        val time = System.currentTimeMillis().toString()
        mDbRef.child(currentUserId!!).child("status").setValue(time) // Update last seen timestamp
    }

    private fun updateTypingStatus(isTyping: Boolean) {
        mDbRef.child(currentUserId!!).child("typing").setValue(isTyping)
    }

    private fun checkUserStatus() {
        mDbRef.child(selectedUserId!!).child("status").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val status = snapshot.getValue(String::class.java)
                if (status == "Active now") {
                    binding.userStatus.text = "Active now"
                } else {
                    val lastSeenTime = status?.toLongOrNull()
                    binding.userStatus.text = if (lastSeenTime != null) {
                        "Last seen ${formatTime(lastSeenTime)}"
                    } else {
                        "Last seen unknown"
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })

        mDbRef.child(selectedUserId!!).child("typing").addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.getValue(Boolean::class.java) == true) {
                    binding.userStatus.text = "Typing..."
                } else {
                    // If not typing, refresh the user's status
                    checkUserStatus() // Refresh user status
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun listenForUserNameChanges() {
        val selectedUserRef = mDbRef.child(selectedUserId!!)
        selectedUserRef.child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newName = snapshot.getValue(String::class.java)
                    if (!newName.isNullOrEmpty()) {
                        // Update UI elements that display the receiver's name
                        binding.userName.text = newName // Update receiver's name in chat
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to update user name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // Listen for the current user's name changes
        val currentUserRef = mDbRef.child(currentUserId!!)
        currentUserRef.child("name").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val newName = snapshot.getValue(String::class.java)
                    if (!newName.isNullOrEmpty()) {
                        // Update the chat messages with the new sender name if necessary
                        updateSenderNameInMessages(newName)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChatActivity, "Failed to update user name: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSenderNameInMessages(newName: String) {
        messages.forEach { message ->
            if (message.senderId == currentUserId) {
                message.senderName = newName // Update sender name in existing messages
            }
        }
        chatAdapter.notifyDataSetChanged() // Notify adapter of the changes
    }

    private fun sendBackChatDataToMainActivity(lastMessage: String = "", timestamp: String = System.currentTimeMillis().toString()) {
        val resultIntent = Intent().apply {
            putExtra("name", receiverName)
            putExtra("last_message", lastMessage)
            putExtra("timestamp", timestamp)
            putExtra("receiver_uid", selectedUserId)
        }
        setResult(RESULT_OK, resultIntent)
    }
}