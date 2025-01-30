package com.example.chatbox_app.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.chat_application.dataclass.User
import com.example.chatbox_app.MainActivity
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        addTextWatchers()

        binding.txtCreate.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val password = binding.edtPass.text.toString().trim()
            val confirmPassword = binding.edtConPass.text.toString().trim()

            if (validateInputs(name, email, password, confirmPassword)) {
                signUpUser(name,email, password)
            }
        }
    }

    private fun addTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val name = binding.edtName.text.toString().trim()
                val email = binding.edtEmail.text.toString().trim()
                val password = binding.edtPass.text.toString().trim()
                val confirmPassword = binding.edtConPass.text.toString().trim()

                // Check if all inputs are valid
                if (validateInputs(name, email, password, confirmPassword)) {
                    binding.txtCreate.setTextColor(Color.WHITE)
                    binding.logBorder.setBackgroundResource(R.drawable.select_log_border) // Green border
                } else {
                    binding.txtCreate.setTextColor(Color.BLACK)
                    binding.logBorder.setBackgroundResource(R.drawable.log_border) // Default border
                }
            }
        }

        binding.edtName.addTextChangedListener(textWatcher)
        binding.edtEmail.addTextChangedListener(textWatcher)
        binding.edtPass.addTextChangedListener(textWatcher)
        binding.edtConPass.addTextChangedListener(textWatcher)
    }

    private fun validateInputs(
        username: String,
        email: String,
        password: String,
        confirmPassword: String
    ): Boolean {
        if (username.isEmpty()) {
            showErrorUnderEditText("Username cannot be empty")
            return false
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showErrorUnderEditText("Invalid email address", isEmail = true)
            return false
        }


        if (confirmPassword.isEmpty() || password != confirmPassword) {
            showErrorUnderEditText("Passwords do not match")
            return false
        }

        binding.errorText.text = "" // Clear previous errors
        return true
    }


    private fun showErrorUnderEditText(message: String, isEmail: Boolean = false) {
        if (isEmail) {
            binding.errorText.text = message
            binding.errorText.setTextColor(Color.RED)
        } else {
            binding.errorTextUsername.text = message
            binding.errorTextUsername.setTextColor(Color.RED)
        }
    }


    private fun signUpUser(name: String, email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Account created successfully", Toast.LENGTH_SHORT).show()

                    val uid = mAuth.currentUser?.uid!!
                    addUserToDatabase(name, email, uid)

                    val intent = Intent(this, MainActivity::class.java).apply {
                        putExtra("user_name", name)
                        putExtra("uid", uid)
                    }
                    startActivity(intent)
                    finish()


                } else {
                    val errorMessage = task.exception?.localizedMessage ?: "Sign-up failed"
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun addUserToDatabase(name: String, email: String, uid: String) {
        mDbRef = FirebaseDatabase.getInstance().getReference()
        mDbRef.child("user").child(uid).setValue(User(name, email, uid))

    }
}
