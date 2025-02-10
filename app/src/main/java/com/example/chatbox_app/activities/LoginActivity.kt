package com.example.chatbox_app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chatbox_app.MainActivity
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance()

        // Add TextWatchers to input fields
        addTextWatchers()

        // Login button click listener
        binding.logBorder.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPass.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }
        binding.logTxt.setOnClickListener {
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPass.text.toString().trim()

            if (validateInputs(email, password)) {
                loginUser(email, password)
            }
        }
    }

    // Add TextWatchers to validate inputs dynamically
    private fun addTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val email = binding.edtEmail.text.toString().trim()
                val password = binding.edtPass.text.toString().trim()
                validateAndUpdateUI(email, password)
            }

            override fun afterTextChanged(s: Editable?) {}
        }

        binding.edtEmail.addTextChangedListener(textWatcher)
        binding.edtPass.addTextChangedListener(textWatcher)
    }

    // Validate inputs and update UI
    private fun validateAndUpdateUI(email: String, password: String) {
        val isValid = email.isNotEmpty() && password.isNotEmpty()
        if (isValid) {
            binding.logTxt.setTextColor(Color.WHITE)
            binding.logBorder.setBackgroundResource(R.drawable.select_log_border) // Green border
        } else {
            binding.logTxt.setTextColor(Color.BLACK)
            binding.logBorder.setBackgroundResource(R.drawable.log_border) // Default border
        }
    }

    // Validate inputs before login
    private fun validateInputs(email: String, password: String): Boolean {
        return when {
            email.isEmpty() -> {
                binding.edtEmail.error = "Email is required"
                false
            }
            password.isEmpty() -> {
                binding.edtPass.error = "Password is required"
                false
            }
            else -> true
        }
    }

    // Login user with Firebase Authentication
    // LoginActivity
    private fun loginUser(email: String, password: String) {
        binding.logBorder.isEnabled = false
        binding.logProgress.visibility = android.view.View.VISIBLE

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                binding.logProgress.visibility = android.view.View.GONE
                binding.logBorder.isEnabled = true

                if (task.isSuccessful) {
                    // Get current user's display name and uid
                    val currentUser = mAuth.currentUser
                    val name = currentUser?.displayName ?: "No Name" // Using displayName if available
                    val userId = currentUser?.uid ?: ""

                    // Navigate to MainActivity
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    // Pass userName and userId to MainActivity via Intent
                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("user_name", name) // Pass user's display name
                        putExtra("uid", userId) // Pass user's uid
                    }

                    startActivity(intent)
                    finish() // Close LoginActivity
                } else {
                    // Show error message
                    val errorMessage = task.exception?.localizedMessage ?: "Login failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

}
