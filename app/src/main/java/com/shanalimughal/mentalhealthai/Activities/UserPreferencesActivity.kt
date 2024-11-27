package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.UserPreferencesOnBoradModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityUserPreferencesBinding

class UserPreferencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserPreferencesBinding
    private lateinit var userPreferences: UserPreferencesOnBoradModel

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var etProfession: EditText
    private lateinit var rgMood: RadioGroup
    private lateinit var cbSelfLove: CheckBox
    private lateinit var cbConfidence: CheckBox
    private lateinit var cbProductivity: CheckBox
    private lateinit var cbGratitude: CheckBox
    private lateinit var etSleep: EditText
    private lateinit var rgExerciseFrequency: RadioGroup
    private lateinit var btnSave: Button

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityUserPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // initializing the variables
        initializingVariables()

        // onclick listener on saving preferences
        binding.btnSave.setOnClickListener {
            progressDialog.show()
            savePreferences()
        }
    }

    private fun initializingVariables() {
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // initializing layout views
        etProfession = binding.etProfession
        rgMood = binding.rgMood
        cbSelfLove = binding.cbSelfLove
        cbConfidence = binding.cbConfidence
        cbProductivity = binding.cbProductivity
        cbGratitude = binding.cbGratitude
        etSleep = binding.etSleep
        rgExerciseFrequency = binding.rgExerciseFrequency
        btnSave = binding.btnSave

        // disabling the save button in the start
        binding.btnSave.isEnabled = false

        // Set listeners
        etProfession.addTextChangedListener(inputWatcher)
        rgMood.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgExerciseFrequency.setOnCheckedChangeListener { _, _ -> validateInputs() }

        val checkBoxes = listOf(
            cbSelfLove,
            cbConfidence,
            cbProductivity,
            cbGratitude,
        )
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> validateInputs() }
        }

        // initializing progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
    }

    private fun savePreferences() {
        val profession = etProfession.text.toString()
        val mood = findViewById<RadioButton>(rgMood.checkedRadioButtonId)?.text.toString()
        val goals = mutableListOf<String>().apply {
            if (cbSelfLove.isChecked) add(cbSelfLove.text.toString())
            if (cbConfidence.isChecked) add(cbConfidence.text.toString())
            if (cbProductivity.isChecked) add(cbProductivity.text.toString())
            if (cbGratitude.isChecked) add(cbGratitude.text.toString())
        }
        val sleepHours = etSleep.text.toString().toIntOrNull() ?: 0
        val exerciseFrequency =
            findViewById<RadioButton>(rgExerciseFrequency.checkedRadioButtonId)?.text.toString()

        userPreferences = UserPreferencesOnBoradModel(
            profession, mood, goals, sleepHours, exerciseFrequency
        )

        savePreferencesToSharedPreferences(userPreferences)
        savePreferencesToFirebase(userPreferences)
    }

    private val inputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            validateInputs()
        }
    }

    private fun validateInputs() {
        val isProfessionFilled = etProfession.text.isNotBlank()
        val isMoodSelected = rgMood.checkedRadioButtonId != -1
        val isGoalSelected =
            listOf(cbSelfLove, cbConfidence, cbProductivity, cbGratitude).any { it.isChecked }
        val isSleepHoursFilled = etSleep.text.isNotBlank()
        val isExerciseFrequencySelected = rgExerciseFrequency.checkedRadioButtonId != -1

        btnSave.isEnabled =
            isProfessionFilled && isMoodSelected && isGoalSelected && isSleepHoursFilled && isExerciseFrequencySelected
    }

    private fun savePreferencesToSharedPreferences(userPreferences: UserPreferencesOnBoradModel) {
        editor.putString("profession", userPreferences.profession)
        editor.putString("mood", userPreferences.mood)
        editor.putStringSet("goals", userPreferences.goals.toSet())
        editor.putInt("sleepHours", userPreferences.sleepHours)
        editor.putString("exerciseFrequency", userPreferences.exerciseFrequency)
        editor.apply()
    }

    private fun savePreferencesToFirebase(userPreferences: UserPreferencesOnBoradModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users")
            .child(userId)
            .child("userPreferencesOnBoard")
            .setValue(userPreferences)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Preferences saved successfully", Toast.LENGTH_SHORT)
                        .show()
                    progressDialog.dismiss()

                    editor.putBoolean("isUserPreferencesOnBoard", true)
                    editor.putBoolean("dailyMood", true)
                    editor.apply()

                    databaseReference.child("Users")
                        .child(userId)
                        .child("userPreferencesOnBoard")
                        .child("isUserPreferencesOnBoard")
                        .setValue(true)

                    settingBooleanValuesOnFb()

                    // going to main activity now
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        this,
                        "Failed to save preferences, try again",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                }
            }
    }

    private fun settingBooleanValuesOnFb() {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference
        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users")
            .child(userId)
            .child("isFirstTimeAffirmationsGenerated")
            .setValue(false)

        databaseReference.child("Users")
            .child(userId)
            .child("isFirstTimeMeditationsGenerated")
            .setValue(false)

        databaseReference.child("Users")
            .child(userId)
            .child("isFirstTimeNatureAndEnvironmentGenerated")
            .setValue(false)


    }
}
