package com.example.chatbox_app.fragments

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.chatbox_app.R
import com.example.chatbox_app.activities.NotificationActivity
import com.example.chatbox_app.activities.ProfileActivity
import com.example.chatbox_app.databinding.FragmentSettingsBinding
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users") // Ensure correct path

        setupUI()
        return binding.root
    }

    private fun setupUI() {
        // Back button action
        binding.backImg.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Navigate to ProfileActivity
        binding.linearAcc.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        binding.linearUsername.setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }

        // Navigate to NotificationActivity
        binding.notification.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        binding.linearChat.setOnClickListener {
            replaceFragment(MessageFragment()) // Ensure MessageFragment exists
        }

        // Other settings (yet to be implemented)
        binding.chat.setOnClickListener { /* Navigate to Chat Settings */ }
        binding.help.setOnClickListener { /* Navigate to Help Section */ }
        binding.data.setOnClickListener { /* Navigate to Data & Storage Settings */ }
        binding.users.setOnClickListener { /* Invite a friend feature */ }

        // Load user data
        loadUserAccount()
    }

    private fun replaceFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadUserAccount() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Fetch the user data from Firebase
            database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (_binding == null) return // Prevent crash if the fragment is destroyed

                    binding.settingProgress.visibility = View.GONE
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
                        binding.settingsName.text = user?.name ?: "User Name Not Found"
                    } else {
                        binding.settingsName.text = "User Data Not Found"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (_binding != null) {
                        Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        } else {
            Toast.makeText(context, "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}
