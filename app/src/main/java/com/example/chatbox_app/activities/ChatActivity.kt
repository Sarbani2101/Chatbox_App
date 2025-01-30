package com.example.chatbox_app.activities

import Chat
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
import com.example.chatbox_app.dataclass.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var database: DatabaseReference
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

        mDbRef = FirebaseDatabase.getInstance().getReference("user")

        // Initialize Firebase references
        database = FirebaseDatabase.getInstance().reference
        currentUserId = FirebaseAuth.getInstance().currentUser?.uid
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
                // Toggle visibility of sendButton and microImg based on input
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
    private fun sendMessage(messageText: String) {
        if (chatId == null || currentUserId == null) return

        // Create message object
        val message = Message(
            senderId = currentUserId!!,
            message = messageText,
            timestamp = System.currentTimeMillis().toString()
        )

        val messageId = database.child("chats").child(chatId!!).push().key
        if (messageId != null) {
            // Save the message in Firebase Database
            database.child("chats").child(chatId!!).child(messageId).setValue(message)
                .addOnSuccessListener {
                    // Clear the message input field after sending the message
                    binding.messageEditText.text.clear()

                    // Send back the last message and timestamp to update the chat list
                    sendBackChatDataToMainActivity(message.message, message.timestamp)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to send message.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateChatList(message: String, timestamp: Long) {
        // Create chat object for updating the chat list
        val chat = Chat(
            receiverName = receiverName ?: "Unknown User",
            receiverUid = selectedUserId ?: "",
            lastMessage = message,
            timestamp = timestamp
        )

        // Add or update chat in sender's chat list
        mDbRef.child("chats").child(currentUserId ?: "").child(selectedUserId ?: "").setValue(chat)

        // Add or update chat in receiver's chat list
        mDbRef.child("chats").child(selectedUserId ?: "").child(currentUserId ?: "").setValue(chat)
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
