package com.example.chatbox_app

import android.os.Bundle
import android.os.StrictMode
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.chatbox_app.databinding.ActivityMainBinding
import com.example.chatbox_app.fragments.CallsFragment
import com.example.chatbox_app.fragments.ContactsFragment
import com.example.chatbox_app.fragments.MessageFragment
import com.example.chatbox_app.fragments.SettingsFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyLog()
            .build()
        )

        val openFragment = intent.getStringExtra("openFragment")
        if (openFragment == "MessageFragment") {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MessageFragment())
                .commit()
        }

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

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (currentFragment is SettingsFragment ||
            currentFragment is ContactsFragment ||
            currentFragment is CallsFragment) {
            replaceFragment(MessageFragment()) // Navigate back to MessageFragment
        } else {
            super.onBackPressed() // Default back behavior (exit the app)
        }
    }

}