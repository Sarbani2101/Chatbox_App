package com.example.chatbox_app.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import android.graphics.Color  // Corrected Import
import android.location.Address
import android.location.Geocoder
import android.media.audiofx.BassBoost
import android.net.Uri
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.example.chatbox_app.MainActivity
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ActivitySignupBinding
import com.example.chatbox_app.dataclass.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale
import java.util.regex.Pattern


class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var userAddress: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase authentication & database reference
        auth = FirebaseAuth.getInstance()
        mDbRef = FirebaseDatabase.getInstance().getReference("users")

        // Initialize location provider
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Add text watchers to validate input fields
        addTextWatchers()

        // Request location permission
        requestUserLocation()

        // Sign-up button click listeners
        binding.signBorder.setOnClickListener { handleSignup() }
        binding.txtCreate.setOnClickListener { handleSignup() }
    }

    private fun handleSignup() {
        val name = binding.edtName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val pass = binding.edtPass.text.toString().trim()
        val conPass = binding.edtConPass.text.toString().trim()

        if (email.validateInputs(pass, conPass)) {
            signUpUser(name, email, pass)
        }
    }

    private fun addTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateAndUpdateUI()
            }

            override fun afterTextChanged(s: Editable?) {
                validateAndUpdateUI()
            }
        }

        binding.edtName.addTextChangedListener(textWatcher)
        binding.edtEmail.addTextChangedListener(textWatcher)
        binding.edtPass.addTextChangedListener(textWatcher)
        binding.edtConPass.addTextChangedListener(textWatcher)
    }

    private fun validateAndUpdateUI() {
        val name = binding.edtName.text.toString().trim()
        val email = binding.edtEmail.text.toString().trim()
        val pass = binding.edtPass.text.toString().trim()
        val conPass = binding.edtConPass.text.toString().trim()

        val isValid = name.isNotEmpty() && email.isNotEmpty() && pass.isNotEmpty() && conPass.isNotEmpty()
        binding.txtCreate.setTextColor(if (isValid) android.graphics.Color.WHITE else android.graphics.Color.BLACK)
        binding.signBorder.setBackgroundResource(if (isValid) R.drawable.select_log_border else R.drawable.log_border)
    }

    private fun String.isValidPassword(): Boolean {
        val passwordPattern = "^.{6,}\$"
        return Pattern.compile(passwordPattern).matcher(this).matches()
    }

    private fun String.validateInputs(pass: String, conPass: String): Boolean {
        var isValid = true

        // Email validation
        binding.errorTextEmail.text = when {
            isEmpty() -> {
                isValid = false
                "Email is required"
            }
            !Patterns.EMAIL_ADDRESS.matcher(this).matches() -> {
                isValid = false
                "Invalid email address"
            }
            else -> ""
        }

        // Password validation
        binding.errorPassText.text = when {
            pass.isEmpty() -> {
                isValid = false
                "Password is required"
            }
            !pass.isValidPassword() -> {
                isValid = false
                "Password must have at least 6 characters"
            }

            else -> ""
        }

        binding.errorConPassText.text = when{
            pass != conPass -> {
                isValid = false
                "Passwords do not match"
            }

            else -> ""
        }

        return isValid
    }

    private fun requestUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            getLastLocation()
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            getLastLocation()
        } else {
            Toast.makeText(this, "Location permission is required for sign-up", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getLastLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                convertLatLngToAddress(currentLat!!, currentLng!!)
            } else {
                binding.edtLoc.setText("Location unavailable")
            }
        }.addOnFailureListener {
            binding.edtLoc.setText("Failed to get location")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun convertLatLngToAddress(latitude: Double, longitude: Double) {
        val geocoder = Geocoder(this, Locale.getDefault())
        Thread {
            try {
                val addresses: List<Address> = geocoder.getFromLocation(latitude, longitude, 1) ?: emptyList()
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val fullAddress = "${address.featureName ?: "Unknown"}, ${address.locality ?: "Unknown City"}, ${address.countryName ?: "Unknown Country"} - ${address.postalCode ?: "No Postal Code"}"
                    userAddress = fullAddress

                    runOnUiThread {
                        binding.edtLoc.setText(fullAddress)
                    }
                } else {
                    runOnUiThread {
                        binding.edtLoc.setText("Address not found")
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    binding.edtLoc.setText("Address retrieval failed")
                }
            }
        }.start()
    }

    private fun signUpUser(name: String, email: String, password: String) {
        binding.signBorder.isEnabled = false
        binding.signProgress.visibility = View.VISIBLE  // Show loading

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            binding.signProgress.visibility = View.GONE  // Hide loading
            binding.signBorder.isEnabled = true

            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid!!
                addUserToDatabase(name, email, uid, currentLat, currentLng, userAddress)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, task.exception?.localizedMessage ?: "Sign-up failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun addUserToDatabase(name: String, email: String, uid: String, lat: Double?, lng: Double?, address: String?) {
        val user = User(name, "City", "Status", email, uid, lat ?: 0.0, lng ?: 0.0, address ?: "", "")
        mDbRef.child(uid).setValue(user)
    }
}