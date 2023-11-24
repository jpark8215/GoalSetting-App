package com.example.jieungoalsettingapp.ui.home

import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<SpannableString>().apply {
        val boldText = "GoalCraft"
        val spannable = SpannableString(boldText)

        // Apply bold style
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Apply custom font
        spannable.setSpan(TypefaceSpan("Roboto-Regular.ttf"), 0, boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        // Apply text size
        spannable.setSpan(android.text.style.RelativeSizeSpan(1.5f), 0, boldText.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        value = spannable
    }

    val text: LiveData<SpannableString> = _text

}