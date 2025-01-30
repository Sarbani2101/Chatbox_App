package com.example.chatbox_app.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.chatbox_app.databinding.ActivityNotificationBinding

class NotificationActivity : AppCompatActivity() {
    private lateinit var binding: ActivityNotificationBinding
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // Load saved settings
        loadSettings()

        // Dark Mode Toggle
        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            toggleDarkMode(isChecked)
        }

        // Notifications Toggle
        binding.switchChats.setOnCheckedChangeListener { _, isChecked ->
            savePreference("chat_notifications", isChecked)
        }
        binding.switchGroups.setOnCheckedChangeListener { _, isChecked ->
            savePreference("group_notifications", isChecked)
        }
        binding.switchContacts.setOnCheckedChangeListener { _, isChecked ->
            savePreference("contact_notifications", isChecked)
        }

        // Manage Account Actions
        binding.btnUpdatePhone.setOnClickListener {
            Toast.makeText(this, "Update Phone feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.btnUpdateEmail.setOnClickListener {
            Toast.makeText(this, "Update Email feature coming soon!", Toast.LENGTH_SHORT).show()
        }
        binding.btnUpdatePassword.setOnClickListener {
            Toast.makeText(this, "Update Password feature coming soon!", Toast.LENGTH_SHORT).show()
        }

        // Sync Contacts
        binding.btnSyncContacts.setOnClickListener {
            Toast.makeText(this, "Syncing contacts...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSettings() {
        binding.switchDarkMode.isChecked = sharedPreferences.getBoolean("dark_mode", false)
        binding.switchChats.isChecked = sharedPreferences.getBoolean("chat_notifications", true)
        binding.switchGroups.isChecked = sharedPreferences.getBoolean("group_notifications", true)
        binding.switchContacts.isChecked = sharedPreferences.getBoolean("contact_notifications", true)
    }

    private fun savePreference(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    private fun toggleDarkMode(isChecked: Boolean) {
        savePreference("dark_mode", isChecked)
        AppCompatDelegate.setDefaultNightMode(
            if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
        recreate()
    }
}