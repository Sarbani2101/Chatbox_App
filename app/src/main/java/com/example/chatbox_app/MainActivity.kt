package com.example.chatbox_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chatbox_app.databinding.ActivityMainBinding
import com.example.chatbox_app.fragments.CallsFragment
import com.example.chatbox_app.fragments.ContactsFragment
import com.example.chatbox_app.fragments.FriendListFragment
import com.example.chatbox_app.fragments.MessageFragment
import com.example.chatbox_app.fragments.SettingsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load default fragment on first launch
        if (savedInstanceState == null) {
            replaceFragment(MessageFragment())
        }

        // Handle bottom navigation clicks
        binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
            val fragment: Fragment = when (menuItem.itemId) {
                R.id.nav_message -> MessageFragment()
                R.id.nav_friend_list -> FriendListFragment()
                R.id.nav_calls -> CallsFragment()
                R.id.nav_contacts -> ContactsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> MessageFragment()
            }
            replaceFragment(fragment)
            true
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commitNow() // use commitNow to avoid fragment manager back stack build-up
    }
}