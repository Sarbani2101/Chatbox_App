data class Chat(
    val senderId: String = "",
    val receiverUid: String = "",
    val senderName: String = "",
    val receiverName: String = "",
    var lastMessage: String = "",
    var timestamp: Long = 0L,
    val profileImageUrl: String = ""
)