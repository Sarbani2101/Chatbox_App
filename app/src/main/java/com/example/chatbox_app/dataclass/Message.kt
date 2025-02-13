package com.example.chatbox_app.dataclass

data class Message(
    val senderId: String = "",
    var senderName: String = "",
    val message: String = "",
    val timestamp: String = "",
    val receiverId : String = "",
    val messageType: String = "text", // "text", "image", "video", "audio"
    val mediaUrl: String = "",  // URL for media (image, video, audio)
    val status: String = "sent"  // "sent", "delivered", "read"
)



