package com.shanalimughal.mentalhealthai.Fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Activities.MainActivity
import com.shanalimughal.mentalhealthai.BuildConfig
import com.shanalimughal.mentalhealthai.Models.TherapySummaryModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentTherapySummaryBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class TherapySummaryFragment : Fragment() {

    private lateinit var binding: FragmentTherapySummaryBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var aiQuestions: MutableSet<String>
    private lateinit var feedback: String

    private lateinit var progressDialog: ProgressDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTherapySummaryBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        aiQuestions =
            sharedPreferences.getStringSet("aiQuestions", mutableSetOf()) ?: mutableSetOf()
        feedback = sharedPreferences.getString("feedback", "").toString()

        // getting session summary now.
        getSessionSummary(aiQuestions, feedback)


        val progressBar = requireActivity().findViewById<ProgressBar>(R.id.progressBar)
        progressBar.progress = 100

        binding.saveAndContinueBtn.setOnClickListener {
            uploadToFirebase()
        }

        binding.regenerateBtn.setOnClickListener {
            // re-generating session summary again because of error.
            getSessionSummary(aiQuestions, feedback)
        }

        return binding.root
    }

    private fun getSessionSummary(aiQuestions: MutableSet<String>, feedback: String) {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Generating Summary...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val model = GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 1000
                responseMimeType = "text/plain"
            },
            safetySettings = listOf(
                SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.MEDIUM_AND_ABOVE),
                SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.MEDIUM_AND_ABOVE),
            ),
        )
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = model.startChat().sendMessage(
                    "We had a QnA session. You are my Mental Health Therapist. So here's all the discussion we had just now. You have to write a summary on this discussion." +
                            " Write some beautiful and amazing summary because session is now ended." +
                            "\nAlso remember don't refer me as 'he' always refer me as 'you' in the summary okay!!\n\nThis Session Discussion:\n$aiQuestions"
                )

                val updatedResponse = response.text.toString().replace("*", "")
                    .replace("**", "")

                withContext(Dispatchers.Main) {
                    try {
                        progressDialog.dismiss()
                        binding.regenerateBtn.visibility = View.GONE
                        binding.saveAndContinueBtn.visibility = View.VISIBLE
                        binding.summaryText.text = "$updatedResponse\n\nYour Feedback:\n$feedback"
                    } catch (e: Exception) {
                        e.message
                    }
                }
            } catch (e: Exception) {
                Looper.prepare()
                Toast.makeText(
                    requireContext(),
                    "Some error occurred, try again",
                    Toast.LENGTH_SHORT
                ).show()
                progressDialog.dismiss()
                binding.regenerateBtn.visibility = View.VISIBLE
                binding.saveAndContinueBtn.visibility = View.GONE
                binding.summaryText.text = "Some error occurred, check your internet connection and re-generate the summary"
                e.printStackTrace()
                Log.d("apiError", e.message ?: "Error occurred")
            }
        }
    }

    private fun uploadToFirebase() {
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving...")
        progressDialog.setCancelable(false)
        progressDialog.show()

        val currentTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val formattedTime = currentTime.format(formatter)

        val summary = binding.summaryText.text.toString()

        val model = TherapySummaryModel(summary, formattedTime)

        FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .child("therapySessions")
            .push()
            .setValue(model)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Session saved successfully", Toast.LENGTH_SHORT)
                    .show()
                progressDialog.dismiss()
                val intent = Intent(requireContext(), MainActivity::class.java)
                startActivity(intent)
                requireActivity().finish()

                editor.remove("aiQuestions")
                editor.putString("summary", summary)
                editor.putString("therapyDate", formattedTime)
                editor.apply()

            }.addOnFailureListener {
                progressDialog.dismiss()
            }
    }
}