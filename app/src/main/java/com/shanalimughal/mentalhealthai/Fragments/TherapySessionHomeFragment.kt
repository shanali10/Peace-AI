package com.shanalimughal.mentalhealthai.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentTherapySessionHomeBinding

class TherapySessionHomeFragment : Fragment() {
    private lateinit var binding: FragmentTherapySessionHomeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentTherapySessionHomeBinding.inflate(inflater, container, false)

        val progressBar = requireActivity().findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = 25

        binding.continueBtn.setOnClickListener {
            val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
            fragmentTransaction.replace(R.id.linearLayoutTherapy, TherapyQuestionsFragment())
            fragmentTransaction.commit()
        }
        return binding.root
    }
}