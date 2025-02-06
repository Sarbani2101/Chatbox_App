package com.example.chatbox_app.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.chatbox_app.R
import com.example.chatbox_app.dataclass.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    private lateinit var locationText: TextView
    private lateinit var nameText: TextView
    private lateinit var nameTextView: TextView
    private lateinit var emailTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var backButton: ImageView
    private lateinit var progressBar: ProgressBar

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Initialize Firebase Auth and Database Reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("users") // Check if path is correct

        // Bind views
        nameText = findViewById(R.id.name)
        nameTextView = findViewById(R.id.nameUser)
        emailTextView = findViewById(R.id.emailUser)
        profileImageView = findViewById(R.id.userProfile)
        backButton = findViewById(R.id.backBtn)
        locationText = findViewById(R.id.locAddress)
        progressBar = findViewById(R.id.progressBar)

        // Load user profile from Firebase
        loadUserProfile()

        // Open image picker on button click
        profileImageView.setOnClickListener {
            openImagePicker()
        }

        // Back button functionality
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri = data.data
            val inputStream = contentResolver.openInputStream(imageUri!!)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Upload the image to Firebase as Base64
            uploadProfileImage(bitmap)
        }
    }

    private fun uploadProfileImage(bitmap: Bitmap) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)

            // Save the Base64 image string to Firebase
            database.child(userId).child("profileImage").setValue(base64Image)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Profile image updated.", Toast.LENGTH_SHORT).show()
                        Glide.with(this).load(bitmap).into(profileImageView) // Update UI
                    } else {
                        Toast.makeText(this, "Failed to update profile image.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            database.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)

                        if (user != null) {
                            nameTextView.text = user.name.ifEmpty { "N/A" }
                            emailTextView.text = user.email.ifEmpty { "N/A" }
                            nameText.text = user.name.ifEmpty { "N/A" }
                            locationText.text = user.address.ifEmpty { "N/A" }

                            // Load profile image from Base64
                            if (!user.profileImage.isNullOrEmpty()) {
                                try {
                                    val decodedBytes = Base64.decode(user.profileImage, Base64.DEFAULT)
                                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                                    profileImageView.setImageBitmap(bitmap)
                                } catch (e: Exception) {
                                    profileImageView.setImageResource(R.drawable.ic_default_profile_image)
                                }
                            } else {
                                profileImageView.setImageResource(R.drawable.ic_default_profile_image)
                            }
                        } else {
                            Toast.makeText(this@ProfileActivity, "User data is null.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@ProfileActivity, "User data not found.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ProfileActivity, "Failed to load user data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}
