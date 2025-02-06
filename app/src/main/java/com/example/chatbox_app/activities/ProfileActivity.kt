package com.example.chatbox_app.activities

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chat_application.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var nameText: TextView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var backButton: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("user") // Ensure this matches your Firebase database structure

        // Bind views
        nameText = findViewById(R.id.name)
        nameTextView = findViewById(R.id.nameUser)  // Ensure this ID matches your layout file
        emailTextView = findViewById(R.id.emailUser) // Ensure this ID matches your layout file
        profileImageView = findViewById(R.id.userProfile) // Ensure this ID matches your layout file
        backButton = findViewById(R.id.backBtn) // Ensure this ID matches your layout file

        // Load user profile from Firebase
        loadUserProfile()

        // Back button functionality
        backButton.setOnClickListener {
            finish() // Closes the activity and returns to the previous screen
        }
    }

    private fun loadUserProfile() {
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
                            nameTextView.text = user.name
                            emailTextView.text = user.email
                            nameText.text = user.name


                            // Load the profile image using Glide (if it exists)
                            if (user.profileImage!!.isNotEmpty()) {
                                Glide.with(this@ProfileActivity)
                                    .load(user.profileImage)
                                    .placeholder(R.drawable.ic_default_profile_image) // Placeholder image
                                    .into(profileImageView)
                            } else {
                                // Set a default image if no profile image exists
                                profileImageView.setImageResource(R.drawable.ic_default_profile_image)
                            }
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "User data not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database error
                    Toast.makeText(this@ProfileActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}