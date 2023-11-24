// Import necessary packages and classes
package com.example.jieungoalsettingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.jieungoalsettingapp.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

// Declare the MainActivity class, extending AppCompatActivity
class MainActivity : AppCompatActivity() {

    // Declare a late-initialized variable for view binding
    private lateinit var binding: ActivityMainBinding

    // The entry point of the activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get a reference to the BottomNavigationView
        val navView: BottomNavigationView = binding.navView

        // Get the NavController associated with the NavHostFragment in the activity's layout
        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        // Define the top level destinations for the ActionBar
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_dashboard, R.id.navigation_notifications
            )
        )

        // Set up the ActionBar with the NavController and AppBarConfiguration
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Connect the BottomNavigationView with the NavController for navigation
        navView.setupWithNavController(navController)
    }

    // Override the onSupportNavigateUp function to handle Up navigation
    override fun onSupportNavigateUp(): Boolean {
        // Get the NavController associated with the NavHostFragment
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Perform Up navigation or call the default behavior
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Override the onBackPressed function to handle custom back navigation
    override fun onBackPressed() {
        // Get the NavController associated with the NavHostFragment
        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Check if the back stack is empty
        if (!navController.popBackStack()) {
            // If the back stack is empty, call the default onBackPressed behavior
            super.onBackPressed()
        }
    }
}
