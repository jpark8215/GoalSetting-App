package com.example.jieungoalsettingapp.ui.home

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.google.android.material.textfield.TextInputEditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.jieungoalsettingapp.R
import com.example.jieungoalsettingapp.databinding.FragmentHomeBinding
import java.net.URLEncoder

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var editTextUserInput: TextInputEditText
    private lateinit var buttonSubmit: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize the editTextUserInput and buttonSubmit
        editTextUserInput = _binding?.textInput?.editText as? TextInputEditText ?: return root
        buttonSubmit = _binding?.buttonSubmit ?: return root


        // Set a click listener for the "Submit" button
        buttonSubmit.setOnClickListener {
            val userInput = editTextUserInput.text.toString()
            performGoogleSearch(userInput)
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



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find the "Submit" button by its ID
        val buttonSubmit: Button = view.findViewById(R.id.buttonSubmit)
        // Find the TextInputEditText by its ID
        val textInputEditText: TextInputEditText = view.findViewById(R.id.editTextUserInput)


        // Set a click listener for the "Submit" button
        buttonSubmit.setOnClickListener {
            val userInput = textInputEditText.text.toString()
            performGoogleSearch(userInput)
        }
    }



    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
