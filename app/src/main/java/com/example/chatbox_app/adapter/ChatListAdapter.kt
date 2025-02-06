import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ChatListBinding
import com.example.chatbox_app.dataclass.Chat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatListAdapter(private val chatList: MutableList<Chat>, private val onChatClick: (Chat) -> Unit) :
    RecyclerView.Adapter<ChatListAdapter.ChatViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ChatListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chatList.size

    inner class ChatViewHolder(private val binding: ChatListBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            binding.chatUserName.text = chat.receiverName
            binding.chatMessagePreview.text = chat.lastMessage
            binding.chattime.text = formatTimestamp(chat.timestamp)

            // Use Glide to load the profile image
            Glide.with(binding.root.context)
                .load(chat.profileImageUrl) // Ensure that profileImageUrl is part of your Chat object.
                .placeholder(R.drawable.ic_default_profile_image)
                .into(binding.userProfileImage)

            binding.root.setOnClickListener {
                onChatClick(chat)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val date = Date(timestamp)
            val format = SimpleDateFormat("HH:mm", Locale.getDefault())
            return format.format(date)
        }
    }
}
