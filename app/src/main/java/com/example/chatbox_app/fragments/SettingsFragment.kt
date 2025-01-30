package com.example.chatbox_app.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.chat_application.dataclass.User
import com.example.chatbox_app.R
import com.example.chatbox_app.activities.NotificationActivity
import com.example.chatbox_app.activities.ProfileActivity
import com.example.chatbox_app.databinding.FragmentSettingsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

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
        database = FirebaseDatabase.getInstance().getReference("user") // Ensure this matches your Firebase database structure

        setupUI()
        return binding.root
    }

    private fun setupUI() {
        binding.backImg.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.account.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.chat.setOnClickListener {
            // Navigate to Chat Settings
        }

        binding.notification.setOnClickListener {
            val intent = Intent(requireContext(), NotificationActivity::class.java)
            startActivity(intent)
        }

        binding.help.setOnClickListener {
            // Navigate to Help Section
        }

        binding.data.setOnClickListener {
            // Navigate to Data & Storage Settings
        }

        binding.users.setOnClickListener {
            // Invite a friend feature
        }
        loadUserAccount()
    }

    private fun loadUserAccount() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            // Fetch the user data from Firebase
            database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Map the snapshot to the User data class
                        val user = snapshot.getValue(User::class.java)
                        if (user != null) {
                            // Set the name and email in the respective TextViews
                            binding.settingsName.text = user.name

                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(context, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(context, "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
