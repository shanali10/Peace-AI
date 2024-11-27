package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.Context
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.UpdateUserInfoModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityPersonalInfoSettingsBinding

class PersonalInfoSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPersonalInfoSettingsBinding
    private lateinit var userPreferences: UpdateUserInfoModel

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var etUserName: EditText
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

    // shared preferences details
    private lateinit var getUserName: String
    private lateinit var getUserProfession: String
    private lateinit var getUserMood: String
    private lateinit var getUserPersonalGoals: MutableSet<String>
    private var getUserSleepHours: Int? = 0
    private lateinit var getExceriseFrequency: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPersonalInfoSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // initializing the variables
        initializingVariables()

        // getting user preferences
        getUserPreferences()

        // onclick listener on saving preferences
        binding.btnSave.setOnClickListener {
            progressDialog.show()
            savePreferences()
        }

        binding.backArrow.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initializingVariables() {
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // initializing layout views
        etUserName = binding.etUserName
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
        etUserName.addTextChangedListener(inputWatcher)
        etProfession.addTextChangedListener(inputWatcher)
        rgMood.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgExerciseFrequency.setOnCheckedChangeListener { _, _ -> validateInputs() }

        val checkBoxes = listOf(
            cbSelfLove,
            cbConfidence,
            cbProductivity,
            cbGratitude
        )
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> validateInputs() }
        }

        // initializing progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
    }

    private fun getUserPreferences() {
        getUserName = sharedPreferences.getString("userName", "") ?: ""
        getUserProfession = sharedPreferences.getString("profession", "") ?: ""
        getUserMood = sharedPreferences.getString("mood", "") ?: ""
        getUserPersonalGoals =
            sharedPreferences.getStringSet("goals", emptySet())?.toMutableSet() ?: mutableSetOf()
        getUserSleepHours = sharedPreferences.getInt("sleepHours", 0)
        getExceriseFrequency = sharedPreferences.getString("exerciseFrequency", "") ?: ""

        // displaying user preferences
        displayUserPreferences()
    }

    private fun displayUserPreferences() {
        etUserName.setText(getUserName)
        etProfession.setText(getUserProfession)

        when (getUserMood) {
            getString(R.string.happy) -> binding.rbHappy.isChecked = true
            getString(R.string.sad) -> binding.rbSad.isChecked = true
            getString(R.string.anxious) -> binding.rbAnxious.isChecked = true
            getString(R.string.stressed) -> binding.rbStressed.isChecked = true
            getString(R.string.calm) -> binding.rbCalm.isChecked = true
            getString(R.string.tired) -> binding.rbTired.isChecked = true
        }

        cbSelfLove.isChecked = getUserPersonalGoals.contains(cbSelfLove.text.toString())
        cbConfidence.isChecked = getUserPersonalGoals.contains(cbConfidence.text.toString())
        cbProductivity.isChecked = getUserPersonalGoals.contains(cbProductivity.text.toString())
        cbGratitude.isChecked = getUserPersonalGoals.contains(cbGratitude.text.toString())

        etSleep.setText(getUserSleepHours.toString())

        when (getExceriseFrequency) {
            getString(R.string.daily) -> binding.rbDailyExercise.isChecked = true
            getString(R.string.once_a_week) -> binding.rbWeeklyExercise.isChecked = true
            getString(R.string.a_few_times_a_week) -> binding.rbFewTimesWeek.isChecked = true
            getString(R.string.rarely_or_never) -> binding.rbNoExercise.isChecked = true
        }
    }

    private fun savePreferences() {
        val name = etUserName.text.toString()
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

        userPreferences = UpdateUserInfoModel(
           name, profession, mood, goals, sleepHours, exerciseFrequency
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

    private fun savePreferencesToSharedPreferences(userPreferences: UpdateUserInfoModel) {
        editor.putString("userName", userPreferences.userName)
        editor.putString("profession", userPreferences.profession)
        editor.putString("mood", userPreferences.mood)
        editor.putStringSet("goals", userPreferences.goals.toSet())
        editor.putInt("sleepHours", userPreferences.sleepHours)
        editor.putString("exerciseFrequency", userPreferences.exerciseFrequency)
        editor.apply()
    }

    private fun savePreferencesToFirebase(userPreferences: UpdateUserInfoModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        // updating just userName
        databaseReference.child("Users")
            .child(userId)
            .child("name")
            .setValue(userPreferences.userName)

        // updating other values also userName under userPreferencesOnBoard child
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
                    editor.apply()

                    databaseReference.child("Users")
                        .child(userId)
                        .child("userPreferencesOnBoard")
                        .child("isUserPreferencesOnBoard")
                        .setValue(true)

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

    override fun onBackPressed() {
        finish()
        super.onBackPressed()
    }
}
