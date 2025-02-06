package com.example.chatbox_app.activities


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("users") // Updated to 'users'
        database = FirebaseDatabase.getInstance().reference
        currentUserId = mAuth.currentUser?.uid
        selectedUserId = intent.getStringExtra("selected_user_id")
        receiverName = intent.getStringExtra("selected_user_name")

        // Generate a unique chat ID (based on user ids)
        chatId = generateChatId()

        if (chatId == null) {
            Toast.makeText(this, "Chat initialization failed", Toast.LENGTH_SHORT).show()
            return
        }

        // Set up user details in the chat header
        binding.userName.text = receiverName ?: "Unknown User"
        binding.userStatus.text = "Active now"

        // Set up RecyclerView for chat messages
        setupRecyclerView()
        loadMessages()

        // Handle back button click
        binding.backImg.setOnClickListener {
            sendBackChatDataToMainActivity()
            finish()  // Optionally finish the current activity
        }

        // Set up message input logic
        setupMessageInput()

        // Handle send button click
        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(messageText)
            }
        }
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
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
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
                } else {
                    binding.sendButton.visibility = View.VISIBLE
                    binding.microImg.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Chat Activity - Sending the message and updating chat list
    private fun sendMessage(message: String) {
        val currentUserId = mAuth.currentUser?.uid ?: return
        val selectedUserId = intent.getStringExtra("selected_user_id") ?: return
        val selectedUserName = intent.getStringExtra("selected_user_name") ?: return

        // Create a Message object
        val chatMessage = Message(
            senderId = currentUserId,
            receiverId = selectedUserId,
            message = message,
            timestamp = System.currentTimeMillis().toString()
        )

        // Save the chat message to Firebase under the chat ID
        database.child("chats").child(chatId!!).push().setValue(chatMessage)
        database.child("chats").child(generateChatId()!!).push().setValue(chatMessage)

        // Save chat summary (last message and timestamp)
        val chatSummary = Chat(
            receiverName = selectedUserName,
            receiverUid = selectedUserId,
            lastMessage = message,
            timestamp = System.currentTimeMillis(),
            isRead = false
        )

        mDbRef.child("chats").child(currentUserId).child(selectedUserId).setValue(chatSummary)
        mDbRef.child("chats").child(selectedUserId).child(currentUserId).setValue(chatSummary)

        // Return to MessageFragment with the new message
        val resultIntent = Intent().apply {
            putExtra("last_message", message)
            putExtra("timestamp", System.currentTimeMillis())
            putExtra("receiver_uid", selectedUserId)
            putExtra("name", selectedUserName)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish() // Finish the ChatActivity after sending the message
    }

    // Send back chat data (last message, timestamp) to the previous activity
    private fun sendBackChatDataToMainActivity(lastMessage: String = "", timestamp: String = System.currentTimeMillis().toString()) {
        val resultIntent = Intent()
        resultIntent.putExtra("name", receiverName ?: "")
        resultIntent.putExtra("last_message", lastMessage)
        resultIntent.putExtra("timestamp", timestamp)
        resultIntent.putExtra("receiver_uid", selectedUserId)
        setResult(RESULT_OK, resultIntent)
    }
}

