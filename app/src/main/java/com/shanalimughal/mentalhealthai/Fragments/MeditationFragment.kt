package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.R
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.shanalimughal.mentalhealthai.Adapters.MeditationAdapter
import com.shanalimughal.mentalhealthai.Models.MeditationModel
import com.shanalimughal.mentalhealthai.databinding.FragmentMeditationBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.properties.Delegates
import com.shanalimughal.mentalhealthai.BuildConfig

class MeditationFragment : Fragment() {
    private var binding: FragmentMeditationBinding? = null
    private lateinit var arrayList: ArrayList<MeditationModel>
    private lateinit var meditationAdapter: MeditationAdapter

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var meditationGoal: MutableSet<String>
    private lateinit var meditationDuration: String
    private lateinit var meditationType: MutableSet<String>

    private lateinit var profession: String
    private lateinit var mood: String
    private lateinit var personalGoals: MutableSet<String>
    private var sleepHours by Delegates.notNull<Int>()

    private lateinit var meditationResponses: MutableSet<String>
    private var isFirstTimeMeditationsGenerated: Boolean = false

    private lateinit var progressDialog: ProgressDialog

    private lateinit var prompt: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMeditationBinding.inflate(inflater, container, false)
        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        meditationGoal =
            sharedPreferences.getStringSet("meditationGoal", mutableSetOf()) ?: mutableSetOf()
        meditationDuration =
            sharedPreferences.getString("preferredMeditationSessionLength", "") ?: ""
        meditationType = sharedPreferences.getStringSet("preferredMeditationTypes", mutableSetOf())
            ?: mutableSetOf()

        profession = sharedPreferences.getString("profession", "").toString()
        mood = sharedPreferences.getString("mood", "").toString()
        personalGoals = sharedPreferences.getStringSet("goals", mutableSetOf())!!
        sleepHours = sharedPreferences.getInt("sleepHours", 0)

        meditationResponses =
            sharedPreferences.getStringSet(
                "meditationResponses",
                emptySet<String?>().toMutableSet()
            ) ?: mutableSetOf()

        isFirstTimeMeditationsGenerated =
            sharedPreferences.getBoolean("isFirstTimeMeditationsGenerated", false)

        arrayList = ArrayList()
        meditationAdapter = MeditationAdapter(arrayList, requireContext())
        binding?.mediationRecycler?.layoutManager = LinearLayoutManager(requireContext())
        binding?.mediationRecycler?.adapter = meditationAdapter

        progressDialog = ProgressDialog(requireContext())
        progressDialog.setCancelable(false)

        if (isFirstTimeMeditationsGenerated) {
            fetchAllMeditationsFromFirebase()
        } else {
            settingArrayListForTheFirstTime()
        }

        binding!!.generateMoreMeditations.visibility = View.INVISIBLE
        binding!!.medIcon.visibility = View.INVISIBLE

        binding!!.generateMoreMeditations.setOnClickListener {
            settingArrayListForTheFirstTime()
        }

        return binding?.root
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun settingArrayListForTheFirstTime() {
        if (arrayList.size < 1) {
            binding!!.meditationErrorText.visibility = View.VISIBLE
            binding!!.meditationErrorText.text = "Loading..."
        } else {
            progressDialog.setMessage("Generating Meditations")
            progressDialog.show()
        }
        for (items in meditationType) {
            val previousMeditations = meditationResponses
            prompt = if (previousMeditations.size > 1) {
                "Suggest a single meditation for the following preferences as a mental health companion:\n\n" +
                        "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                        "Mood: ${mood}\nMeditation Goals: $meditationGoal\nMeditation Type: $items\n" +
                        "Daily Hours of Sleep: ${sleepHours}\nMeditation Duration: ${meditationDuration}\n" +
                        "Provide a detailed description of the meditation including its benefits and steps to follow.\n\n" +
                        "Important Note: Please don't include any of these meditations again.\n" +
                        "Suggest something different than these meditations. These meditations are given below:\n${meditationResponses} "
            } else {
                "Suggest a single meditation for the following preferences as a mental health companion:\n\n" +
                        "Profession: ${profession}\nPersonal Goals: ${personalGoals}\n" +
                        "Mood: ${mood}\nMeditation Goals: $meditationGoal\n" +
                        "Meditation Type: $items\nDaily Hours of Sleep: ${sleepHours}\n" +
                        "Meditation Duration: ${meditationDuration}\n" +
                        "Provide a detailed description of the meditation including its benefits and steps to follow."
            }
            apiRequest(prompt) { responseText ->
                handleApiResponse(responseText)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun handleApiResponse(responseText: String) {
        if (responseText.isNotEmpty()) {
            arrayList.add(
                0,
                MeditationModel(
                    com.shanalimughal.mentalhealthai.R.drawable.meditation_placeholder,
                    responseText,
                    listOf("Meditation Goals: $meditationGoal"),
                    "Daily Hours of Sleep: $sleepHours",
                    "Duration: $meditationDuration",
                    ""
                )
            )

            meditationAdapter.notifyDataSetChanged()
            binding!!.meditationErrorText.visibility = View.INVISIBLE

            // Upload to Firebase
            uploadToFirebase(responseText)


        } else {
            binding!!.meditationErrorText.visibility = View.VISIBLE
            binding!!.mediationRecycler.visibility = View.INVISIBLE
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
                maxOutputTokens = 5000
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
        val myRef = database.getReference("Users").child(userId).child("meditationResponses")

        val responseKey = myRef.push().key
        if (responseKey != null) {
            myRef.child(responseKey).setValue(response)
                .addOnSuccessListener {
                    database.getReference("Users").child(userId)
                        .child("isFirstTimeMeditationsGenerated")
                        .setValue(true)

                    fetchMeditationsFromFirebase()
                }
                .addOnFailureListener {
                    Log.d("MeditationFragmentExceptions", it.message.toString())
                }
        }
    }

    private fun fetchMeditationsFromFirebase() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("meditationResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedMeditationResponses = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    child.getValue(String::class.java)?.let { response ->
                        fetchedMeditationResponses.add(response)
                    }
                }

                arrayList.clear()
                for (snaps in snapshot.children) {
                    val responseValue = snaps.getValue(String::class.java)
                    if (responseValue != null) {
                        arrayList.add(
                            0,
                            MeditationModel(
                                com.shanalimughal.mentalhealthai.R.drawable.meditation_placeholder,
                                responseValue,
                                listOf("Meditation Goals: $meditationGoal"),
                                "Daily Hours of Sleep: $sleepHours",
                                "Duration: $meditationDuration",
                                ""
                            )
                        )
                    }
                }


                // Update SharedPreferences
                meditationResponses.clear()
                meditationResponses.addAll(fetchedMeditationResponses)
                editor.putStringSet("meditationResponses", meditationResponses)
                editor.apply()

                database.getReference("Users")
                    .child(userId)
                    .child("isFirstTimeMeditationsGenerated")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val valueFb = snapshot.value as Boolean
                            if (valueFb) {
                                editor.putBoolean("isFirstTimeMeditationsGenerated", true)
                                editor.apply()
                            } else {
                                editor.putBoolean("isFirstTimeMeditationsGenerated", false)
                                editor.apply()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("MeditationFragmentExceptions", error.message)
                        }
                    })

                progressDialog.dismiss()
                binding!!.generateMoreMeditations.visibility = View.VISIBLE
                binding!!.medIcon.visibility = View.VISIBLE

                binding!!.meditationErrorText.visibility = View.INVISIBLE
                meditationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding!!.meditationErrorText.visibility = View.VISIBLE
                binding!!.mediationRecycler.visibility = View.INVISIBLE
            }
        })
    }

    private fun fetchAllMeditationsFromFirebase() {
        progressDialog.setMessage("Fetching Meditations")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("meditationResponses")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedMeditationResponses = mutableSetOf<String>()
                snapshot.children.forEach { child ->
                    child.getValue(String::class.java)?.let { response ->
                        fetchedMeditationResponses.add(response)
                    }
                }

                arrayList.clear()
                for (snaps in snapshot.children) {
                    val responseValue = snaps.getValue(String::class.java)
                    if (responseValue != null) {
                        arrayList.add(
                            0,
                            MeditationModel(
                                com.shanalimughal.mentalhealthai.R.drawable.meditation_placeholder,
                                responseValue,
                                listOf("Meditation Goals: $meditationGoal"),
                                "Daily Hours of Sleep: $sleepHours",
                                "Duration: $meditationDuration",
                                ""
                            )
                        )
                    }
                }

                // Update SharedPreferences
                meditationResponses.clear()
                meditationResponses.addAll(fetchedMeditationResponses)
                editor.putStringSet("meditationResponses", meditationResponses)
                editor.apply()

                database.getReference("Users")
                    .child(userId)
                    .child("isFirstTimeMeditationsGenerated")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val valueFb = snapshot.value as Boolean
                            if (valueFb) {
                                editor.putBoolean("isFirstTimeMeditationsGenerated", true)
                                editor.apply()
                            } else {
                                editor.putBoolean("isFirstTimeMeditationsGenerated", false)
                                editor.apply()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.d("MeditationFragmentExceptions", error.message)
                        }

                    })

                progressDialog.dismiss()
                binding!!.generateMoreMeditations.visibility = View.VISIBLE
                binding!!.medIcon.visibility = View.VISIBLE

                meditationAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding!!.meditationErrorText.visibility = View.VISIBLE
                binding!!.mediationRecycler.visibility = View.INVISIBLE
            }
        })
    }

    override fun onPause() {
        super.onPause()
        progressDialog.dismiss()
    }
}