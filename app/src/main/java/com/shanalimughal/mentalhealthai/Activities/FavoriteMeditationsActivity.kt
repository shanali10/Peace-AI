package com.shanalimughal.mentalhealthai.Activities

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.shanalimughal.mentalhealthai.Adapters.MeditationAdapter
import com.shanalimughal.mentalhealthai.Models.MeditationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityFavoriteMeditationsBinding

class FavoriteMeditationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteMeditationsBinding
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
    private var sleepHours: Int = 0

    private lateinit var meditationResponses: MutableSet<String>

    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteMeditationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
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

        arrayList = ArrayList()
        meditationAdapter = MeditationAdapter(arrayList, this)
        binding.favoriteMeditationsRecycler.layoutManager = LinearLayoutManager(this)
        binding.favoriteMeditationsRecycler.adapter = meditationAdapter

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        binding.backArrowIcon.setOnClickListener {
            onBackPressed()
        }

        fetchingFavoriteMeditationsFromFirebase()
    }

    private fun fetchingFavoriteMeditationsFromFirebase() {
        progressDialog.setMessage("Fetching Favorite Meditations")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("favoriteMeditations")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
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
                                    R.drawable.meditation_placeholder,
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
                    editor.putStringSet("favoriteMeditations", meditationResponses)
                    editor.apply()

                    progressDialog.dismiss()
                    meditationAdapter.notifyDataSetChanged()
                } else {
                    progressDialog.dismiss()
                    binding.noFavoriteMedText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.noFavoriteMedText.text = "Some error occurred, try again"
                binding.noFavoriteMedText.visibility = View.VISIBLE
            }
        })
    }

    override fun onPause() {
        progressDialog.dismiss()
        super.onPause()
    }

    override fun onResume() {
        fetchingFavoriteMeditationsFromFirebase()
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}