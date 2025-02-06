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
import com.example.chatbox_app.MainActivity
import com.example.chatbox_app.R
import com.example.chatbox_app.databinding.ActivitySignupBinding
import com.example.chatbox_app.dataclass.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.io.IOException
import java.util.Locale


class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var mDbRef: DatabaseReference
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double? = null
    private var currentLng: Double? = null
    private var userAddress: String? = null // Stores converted address

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

        // Request location when activity starts
        requestUserLocation()

        // Sign-up button click listener
        binding.txtCreate.setOnClickListener {
            val name = binding.edtName.text.toString().trim()
            val email = binding.edtEmail.text.toString().trim()
            val pass = binding.edtPass.text.toString().trim()
            val conPass = binding.edtConPass.text.toString().trim()

            if (email.validateInputs(pass, conPass)) {
                signUpUser(name, email, pass)
            }
        }
    }

    private fun addTextWatchers() {
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val name = binding.edtName.text.toString().trim()
                val email = binding.edtEmail.text.toString().trim()
                val pass = binding.edtPass.text.toString().trim()
                val conPass = binding.edtConPass.text.toString().trim()

                validateAndUpdateUI(name, email, pass, conPass)
            }

            override fun afterTextChanged(s: Editable?) {
                val name = binding.edtName.text.toString().trim()
                val email = binding.edtEmail.text.toString().trim()
                val pass = binding.edtPass.text.toString().trim()
                val conPass = binding.edtConPass.text.toString().trim()

                binding.txtCreate.isEnabled = email.validateInputs(pass, conPass)
            }
        }

        binding.edtName.addTextChangedListener(textWatcher)
        binding.edtEmail.addTextChangedListener(textWatcher)
        binding.edtPass.addTextChangedListener(textWatcher)
        binding.edtConPass.addTextChangedListener(textWatcher)
    }


    private fun validateAndUpdateUI(name: String, email: String, pass: String, conPass: String) {
        val isValid = email.isNotEmpty() && pass.isNotEmpty() && conPass.isNotEmpty() && name.isNotEmpty()
        if (isValid) {
            binding.txtCreate.setTextColor(Color.WHITE)
            binding.signBorder.setBackgroundResource(R.drawable.select_log_border) // Green border
        } else {
            binding.txtCreate.setTextColor(Color.BLACK)
            binding.signBorder.setBackgroundResource(R.drawable.log_border) // Default border
        }
    }

    @SuppressLint("SetTextI18n")
    private fun String.validateInputs(pass: String, conPass: String): Boolean {
        var isValid = true

        // Email validation
        if (isEmpty()) {
            binding.errorTextEmail.text = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(this).matches()) {
            binding.errorTextEmail.text = "Invalid email address"
            isValid = false
        } else {
            binding.errorTextEmail.text = "" // Clear error message if valid
        }

        // Password validation
        if (pass.isEmpty()) {
            binding.errorPassText.text = "Password is required"
            isValid = false
        } else if (pass.length < 6) {
            binding.errorPassText.text = "Password must be at least 6 characters"
            isValid = false
        } else {
            binding.errorPassText.text = "" // Clear error message if valid
        }

        // Confirm password validation
        if (conPass.isEmpty()) {
            binding.errorPassText.text = "Confirm password is required"
            isValid = false
        } else if (pass != conPass) {
            binding.errorPassText.text = "Passwords do not match"
            isValid = false
        } else {
            binding.errorPassText.text = "" // Clear error message if valid
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
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
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
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
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