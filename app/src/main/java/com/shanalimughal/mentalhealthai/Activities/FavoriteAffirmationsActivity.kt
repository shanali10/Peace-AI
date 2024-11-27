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
import com.shanalimughal.mentalhealthai.Adapters.AffirmationsAdapter
import com.shanalimughal.mentalhealthai.Models.AffirmationModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityFavoriteAffirmationsBinding

class FavoriteAffirmationsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFavoriteAffirmationsBinding

    private lateinit var arrayList: ArrayList<AffirmationModel>
    private lateinit var affirmationAdapter: AffirmationsAdapter

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var affirmationThemes: MutableSet<String>
    private lateinit var preferredAffirmations: String

    private lateinit var affirmationResponses: MutableSet<String>
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoriteAffirmationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        affirmationThemes = sharedPreferences.getStringSet("affirmationTheme", mutableSetOf())!!
        preferredAffirmations = sharedPreferences.getString("preferredAffirmation", "").toString()

        affirmationResponses =
            sharedPreferences.getStringSet(
                "affirmationResponses",
                emptySet<String?>().toMutableSet()
            ) ?: mutableSetOf()

        arrayList = ArrayList()
        affirmationAdapter = AffirmationsAdapter(arrayList, this@FavoriteAffirmationsActivity)
        binding.favoriteAffirmationRecycler.layoutManager = LinearLayoutManager(this)
        binding.favoriteAffirmationRecycler.adapter = affirmationAdapter

        progressDialog = ProgressDialog(this)
        progressDialog.setCancelable(false)

        // fetching affirmations from firebase and saving in shared preferences
        fetchingFavoriteAffirmationsFromFirebase()

        binding.backArrowIcon.setOnClickListener {
            onBackPressed()
        }

    }

    private fun fetchingFavoriteAffirmationsFromFirebase() {
        progressDialog.setMessage("Fetching Favorite Affirmations")
        progressDialog.show()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("Users")
            .child(userId)
            .child("favoriteAffirmations")

        myRef.addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
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
                            arrayList.add(
                                0,
                                AffirmationModel(
                                    R.drawable.affirmation_placeholder,
                                    responseValue,
                                    listOf("Affirmation Themes: $affirmationThemes"),
                                    "Preferred Affirmations: $preferredAffirmations"
                                )

                            )
                        }
                    }
                    // Update SharedPreferences
                    affirmationResponses.clear()
                    affirmationResponses.addAll(fetchedAffirmationResponses)
                    editor.putStringSet("favoriteAffirmations", fetchedAffirmationResponses)
                    editor.apply()

                    progressDialog.dismiss()
                    affirmationAdapter.notifyDataSetChanged()
                } else {
                    progressDialog.dismiss()
                    binding.noFavortieAffirText.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("MeditationFragmentExceptions", error.message)
                progressDialog.dismiss()
                binding.noFavortieAffirText.text = "Some error occurred, try again"
                binding.noFavortieAffirText.visibility = View.VISIBLE
            }
        })
    }


    override fun onPause() {
        progressDialog.dismiss()
        super.onPause()
    }

    override fun onResume() {
        fetchingFavoriteAffirmationsFromFirebase()
        super.onResume()
    }

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}