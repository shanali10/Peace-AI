package com.shanalimughal.mentalhealthai.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentTherapyFeedbackBinding

class TherapyFeedbackFragment : Fragment() {

    private lateinit var binding: FragmentTherapyFeedbackBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTherapyFeedbackBinding.inflate(inflater, container, false)

        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()


        val progressBar = requireActivity().findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = 75

        binding.submitButton.setOnClickListener {
            val value = binding.feedbackInput.text.toString()
            if (value.isEmpty()) {
                binding.feedbackInput.error = "please write something"
            } else {
                editor.putString("feedback", value)
                editor.apply()

                val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                fragmentTransaction.replace(R.id.linearLayoutTherapy, TherapySummaryFragment())
                fragmentTransaction.commit()
            }
        }
        return binding.root
    }
}