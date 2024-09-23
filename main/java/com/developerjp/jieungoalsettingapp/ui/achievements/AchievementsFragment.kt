package com.developerjp.jieungoalsettingapp.ui.achievements

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.developerjp.jieungoalsettingapp.databinding.FragmentAchievementsBinding
import com.google.android.material.textfield.TextInputEditText
import java.net.URLEncoder

class AchievementsFragment : Fragment() {

    private var _binding: FragmentAchievementsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var editTextUserInput: TextInputEditText
    private lateinit var buttonSubmit: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val achievementsViewModel =
            ViewModelProvider(this).get(AchievementsViewModel::class.java)

        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        editTextUserInput = binding.editTextUserInput
        buttonSubmit = binding.buttonSubmit

        // Set a click listener for the "Submit" button
        buttonSubmit.setOnClickListener {
            val userInput = editTextUserInput.text.toString()
            performGoogleSearch(userInput)
        }

        val textView: TextView = binding.textNotifications
        achievementsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }

    private fun performGoogleSearch(query: String) {
        val encodedQuery = URLEncoder.encode(query, "utf-8")
        val searchUrl = "https://www.google.com/search?q=$encodedQuery"

        // Open the default browser with the search URL
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl))

        try {
            startActivity(browserIntent)
        } catch (e: Exception) {
            showToast("Failed to open the browser.")
            e.printStackTrace()
        }
    }

    private fun showToast(message: String) {
        // Show a short toast message
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}