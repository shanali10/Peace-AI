package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.AffirmationsAdapter
import com.shanalimughal.mentalhealthai.Models.AffirmationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentAffirmationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.shanalimughal.mentalhealthai.BuildConfig

class AffirmationFragment : Fragment() {
    private lateinit var binding: FragmentAffirmationBinding
    private lateinit var arrayList: ArrayList<AffirmationModel>
    private lateinit var affirmationAdapter: AffirmationsAdapter

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor


    private lateinit var profession: String
    private lateinit var mood: String
    private lateinit var personalGoals: MutableSet<String>

    private lateinit var affirmationThemes: MutableSet<String>
    private lateinit var preferredAffirmations: String

    private lateinit var affirmationResponses: MutableSet<String>
    private var isFirstTimeAffirmationsGenerated: Boolean = false

    private lateinit var progressDialog: ProgressDialog
    private lateinit var prompt: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAffirmationBinding.inflate(inflater, container, false)


        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // getting personal information of the user

        profession = sharedPreferences.getString("profession", "").toString()
        mood = sharedPreferences.getString("mood", "").toString()
        personalGoals = sharedPreferences.getStringSet("goals", mutableSetOf())!!

        affirmationThemes = sharedPreferences.getStringSet("affirmationTheme", mutableSetOf())!!
        preferredAffirmations = sharedPreferences.getString("preferredAffirmation", "").toString()

        affirmationResponses =
            sharedPreferences.getStringSet(
                "affirmationResponses",
                emptySet<String?>().toMutableSet()
            ) ?: mutableSetOf()
        isFirstTimeAffirmationsGenerated =
            sharedPreferences.getBoolean("isFirstTimeAffirmationsGenerated", false)

        arrayList = ArrayList()
        affirmationAdapter = AffirmationsAdapter(arrayList, requireContext())
        binding.affirmationRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.affirmationRecycler.adapter = affirmationAdapter


        progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)

        if (isFirstTimeAffirmationsGenerated) {
            fetchAllAffirmationsFromFirebase()
        } else {
            settingArrayListForTheFirstTime()
        }

        binding.generateMoreAffirmations.visibility = View.INVISIBLE
        binding.affirmationIcon.visibility = View.INVISIBLE

        binding.generateMoreAffirmations.setOnClickListener {
            settingArrayListForTheFirstTime()
        }

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun settingArrayListForTheFirstTime() {
        if (arrayList.size < 1) {
            binding.affirmationErrorText.visibility = View.VISIBLE
            binding.affirmationErrorText.text = "Loading..."
        } else {
            progressDialog.setMessage("Generating Affirmations")
            progressDialog.show()
        }
        for (items in affirmationThemes) {
            val previousAffirmations = affirmationResponses
            prompt = if (previousAffirmations.size > 1) {
                "Suggest a single affirmation for the following preferences as a mental health companion:\n\n" +
                        "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                        "Mood: ${mood}\nAffirmation Theme: $items\n" +
                        "Preferred Affirmation Type: ${preferredAffirmations}\n" +
                        "Provide a ${preferredAffirmations} type affirmation in just 200 to 250 characters.\n\n" +
                        "Important Note: Please don't include any of these affirmations again.\n" +
                        "Suggest something different than these affirmations. These affirmations are given below:\n${affirmationResponses} "
            } else {
                "Suggest a single affirmation for the following preferences as a mental health companion:\n\n" +
                        "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                        "Mood: ${mood}\nAffirmation Theme: $items\n" +
                        "Preferred Affirmation Type: ${preferredAffirmations}\n" +
                        "Provide a ${preferredAffirmations} type affirmation in just 200 to 250 characters."
            }
            apiRequest(prompt) { responseText ->
                handleApiResponse(responseText)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleApiResponse(responseText: String) {
        val updatedResponse = responseText.replace("*", "")
            .replace("#", "")
            .replace("\n", " ")

        if (responseText.isNotEmpty()) {
            arrayList.add(
                AffirmationModel(
                    R.drawable.affirmation_placeholder,
                    updatedResponse,
                    listOf("Affirmation Themes: $affirmationThemes"),
                    "Preferred Affirmations: $preferredAffirmations"
                )
            )
            binding.affirmationErrorText.visibility = View.INVISIBLE

            // Upload to Firebase
            uploadToFirebase(responseText)

        } else {
            binding.affirmationErrorText.visibility = View.VISIBLE
            binding.affirmationRecycler.visibility = View.INVISIBLE
            binding.affirmationIcon.visibility = View.INVISIBLE
            binding.generateMoreAffirmations.visibility = View.INVISIBLE
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun apiRequest(prompt: String, callback: (String) -> Unit) {
        val model = GenerativeModel(
            modelName = "gemini-1.5-flash-latest",
            apiKey = BuildConfig.API_KEY,
            generationConfig = generationConfig {
                temperature = 1f
                topK = 64
                topP = 0.95f
                maxOutputTokens = 500
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
                val response = model.startChat(listOf()).sendMessage(prompt)
                withContext(Dispatchers.Main) {
                    response.text?.let { callback(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    callback("")
                }
            }
        }
    }

    private fun uploadToFirebase(response: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users").child(userId).child("affirmationResponses")

        val responseKey = myRef.push().key
        if (responseKey != null) {
            myRef.child(responseKey).setValue(response)
                .addOnSuccessListener {
                    database.getReference("Users").child(userId)
                        .child("isFirstTimeAffirmationsGenerated")
                        .setValue(true)

                    fetchAffirmationsFromFirebase()
                }
                .addOnFailureListener {
                    Log.d("MeditationFragmentExceptions", it.message.toString())
                }
        }
    }

    private fun fetchAffirmationsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("affirmationResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedAffirmationResponses = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    child.getValue(String::class.java)?.let { response ->
                        fetchedAffirmationResponses.add(response)
                    }
                }

                arrayList.clear()
                for (snaps in snapshot.children) {
                    val responseValue = snaps.getValue(String::class.java)
                    if (responseValue != null) {
                        val updatedResponse = responseValue.replace("*", "")
                            .replace("#", "")
                            .replace("\n", " ")

                        arrayList.add(
                            0,
                            AffirmationModel(
                                R.drawable.affirmation_placeholder,
                                updatedResponse,
                                listOf("Affirmation Themes: $affirmationThemes"),
                                "Preferred Affirmations: $preferredAffirmations"
                            )
                        )
                    }
                }

                // Update SharedPreferences
                affirmationResponses.clear()
                affirmationResponses.addAll(fetchedAffirmationResponses)
                editor.putStringSet("affirmationResponses", affirmationResponses)
                editor.apply()

                database.getReference("Users")
                    .child(userId)
                    .child("isFirstTimeAffirmationsGenerated")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val valueFb = snapshot.value as Boolean
                            if (valueFb) {
                                editor.putBoolean("isFirstTimeAffirmationsGenerated", true)
                                editor.apply()
                            } else {
                                editor.putBoolean("isFirstTimeAffirmationsGenerated", false)
                                editor.apply()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("MeditationFragmentExceptions", error.message)
                        }
                    })
                binding.generateMoreAffirmations.visibility = View.VISIBLE
                binding.affirmationIcon.visibility = View.VISIBLE
                affirmationAdapter.notifyDataSetChanged()

                binding.affirmationErrorText.visibility = View.INVISIBLE
                progressDialog.dismiss()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.generateMoreAffirmations.visibility = View.INVISIBLE
                binding.affirmationIcon.visibility = View.INVISIBLE
            }
        })
    }

    private fun fetchAllAffirmationsFromFirebase() {
        progressDialog.setMessage("Fetching Affirmations")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("affirmationResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedAffirmationResponses = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    child.getValue(String::class.java)?.let { response ->
                        fetchedAffirmationResponses.add(response)
                    }
                }

                arrayList.clear()
                for (snaps in snapshot.children) {
                    val responseValue = snaps.getValue(String::class.java)
                    if (responseValue != null) {
                        val updatedResponse = responseValue.replace("*", "")
                            .replace("#", "")
                            .replace("\n", " ")

                        arrayList.add(
                            0,
                            AffirmationModel(
                                R.drawable.affirmation_placeholder,
                                updatedResponse,
                                listOf("Affirmation Themes: $affirmationThemes"),
                                "Preferred Affirmations: $preferredAffirmations"
                            )
                        )
                    }
                }

                // Update SharedPreferences
                affirmationResponses.clear()
                affirmationResponses.addAll(fetchedAffirmationResponses)
                editor.putStringSet("affirmationResponses", fetchedAffirmationResponses)
                editor.apply()

                database.getReference("Users")
                    .child(userId)
                    .child("isFirstTimeAffirmationsGenerated")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val valueFb = snapshot.value as Boolean
                            if (valueFb) {
                                editor.putBoolean("isFirstTimeAffirmationsGenerated", true)
                                editor.apply()
                            } else {
                                editor.putBoolean("isFirstTimeAffirmationsGenerated", false)
                                editor.apply()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("MeditationFragmentExceptions", error.message)
                        }

                    })

                progressDialog.dismiss()
                binding.generateMoreAffirmations.visibility = View.VISIBLE
                binding.affirmationIcon.visibility = View.VISIBLE

                affirmationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.generateMoreAffirmations.visibility = View.INVISIBLE
                binding.affirmationIcon.visibility = View.INVISIBLE
            }
        })
    }

    override fun onPause() {
        progressDialog.dismiss()
        super.onPause()
    }
}