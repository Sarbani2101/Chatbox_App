package com.example.chatbox_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chatbox_app.databinding.ActivityMainBinding
import com.example.chatbox_app.fragments.CallsFragment
import com.example.chatbox_app.fragments.ContactsFragment
import com.example.chatbox_app.fragments.MessageFragment
import com.example.chatbox_app.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load default fragment
        if (savedInstanceState == null) {
            replaceFragment(MessageFragment())
        }

        // Handle bottom navigation clicks
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_message -> replaceFragment(MessageFragment())
                R.id.nav_calls -> replaceFragment(CallsFragment())
                R.id.nav_contacts -> replaceFragment(ContactsFragment())
                R.id.nav_settings -> replaceFragment(SettingsFragment())
            }
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
