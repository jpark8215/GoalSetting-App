package com.example.jieungoalsettingapp.ui.home

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
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        editTextUserInput = _binding?.textInput?.editText as? TextInputEditText ?: return root
        buttonSubmit = _binding?.buttonSubmit ?: return root

        buttonSubmit.setOnClickListener {
            val userInput = editTextUserInput.text.toString()
            performGoogleSearch(userInput)
        }

        val textView: TextView = binding.textHome
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        return root
    }


    private fun performGoogleSearch(query: String) {
        val encodedQuery = URLEncoder.encode(query, "utf-8")
        val searchUrl = "https://www.google.com/search?q=$encodedQuery"
        // You can open this URL in a WebView or a browser to display the Google search results.
        // For simplicity, we'll just show a Toast with the URL.
        showToast("Performing Google search: $searchUrl")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
