package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.AffirmationPreferencesModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityAffirmationSettingsBinding

class AffirmationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAffirmationSettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var cbMotivation: CheckBox
    private lateinit var cbRelaxation: CheckBox
    private lateinit var cbSelfEsteem: CheckBox
    private lateinit var cbPositivity: CheckBox
    private lateinit var rgStyle: RadioGroup
    private lateinit var btnSave: Button

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    // getting values
    private lateinit var getAffirmationTheme: MutableSet<String>
    private lateinit var getPreferredAffirmation: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAffirmationSettingsBinding.inflate(layoutInflater)
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

        binding.backArrow.setOnClickListener{
            onBackPressed()
        }
    }

    private fun initializingVariables() {
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // initializing layout views
        cbMotivation = binding.cbMotivation
        cbRelaxation = binding.cbRelaxation
        cbSelfEsteem = binding.cbSelfEsteem
        cbPositivity = binding.cbPositivity
        rgStyle = binding.rgStyle
        btnSave = binding.btnSave

        // disabling the save button in the start
        binding.btnSave.isEnabled = false

        // Set listeners
        rgStyle.setOnCheckedChangeListener { _, _ -> validateInputs() }

        val checkBoxes = listOf(
            cbMotivation,
            cbRelaxation,
            cbSelfEsteem,
            cbPositivity,
        )
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> validateInputs() }
        }

        // initializing progress dialog
        progressDialog = ProgressDialog(this@AffirmationSettingsActivity)
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
    }

    private fun getUserPreferences() {
        getAffirmationTheme =
            sharedPreferences.getStringSet("affirmationTheme", emptySet())?.toMutableSet()
                ?: mutableSetOf()
        getPreferredAffirmation = sharedPreferences.getString("preferredAffirmation", "") ?: ""

        // displaying user preferences
        displayUserPreferences()
    }

    private fun displayUserPreferences() {
        cbMotivation.isChecked = getAffirmationTheme.contains(cbMotivation.text.toString())
        cbRelaxation.isChecked = getAffirmationTheme.contains(cbRelaxation.text.toString())
        cbSelfEsteem.isChecked = getAffirmationTheme.contains(cbSelfEsteem.text.toString())
        cbPositivity.isChecked = getAffirmationTheme.contains(cbPositivity.text.toString())

        when (getPreferredAffirmation) {
            getString(R.string.short_and_simple) -> binding.rbShortSimple.isChecked = true
            getString(R.string.detailed) -> binding.rbDetailed.isChecked = true
            getString(R.string.story_like) -> binding.rbStoryLike.isChecked = true
        }

    }

    private fun savePreferences() {
        // Get selected radio button values
        val selectedPreferredAffirmationValue =
            binding.root.findViewById<RadioButton>(rgStyle.checkedRadioButtonId).text.toString()

        // Get checked checkbox values for affirmation themes
        val affirmationTheme = mutableListOf<String>().apply {
            if (binding.cbMotivation.isChecked) add(binding.cbMotivation.text.toString())
            if (binding.cbPositivity.isChecked) add(binding.cbPositivity.text.toString())
            if (binding.cbSelfEsteem.isChecked) add(binding.cbSelfEsteem.text.toString())
            if (binding.cbRelaxation.isChecked) add(binding.cbRelaxation.text.toString())
        }

        // Create preferences model
        val affirmationPreferencesModel = AffirmationPreferencesModel(
            affirmationTheme, selectedPreferredAffirmationValue
        )

        // Save preferences to SharedPreferences and Firebase
        savePreferencesToSharedPreferences(affirmationPreferencesModel)
        savePreferencesToFirebase(affirmationPreferencesModel)
    }

    private fun validateInputs() {
        val isAffirmationStyleSelected = rgStyle.checkedRadioButtonId != -1
        val isAffirmationThemeSelected =
            listOf(cbMotivation, cbRelaxation, cbSelfEsteem, cbPositivity).any { it.isChecked }

        btnSave.isEnabled =
            isAffirmationStyleSelected && isAffirmationThemeSelected
    }

    private fun savePreferencesToSharedPreferences(affirmationPreferencesModel: AffirmationPreferencesModel) {
        editor.putStringSet(
            "affirmationTheme",
            affirmationPreferencesModel.affirmationTheme.toSet()
        )
        editor.putString("preferredAffirmation", affirmationPreferencesModel.preferredAffirmation)
        editor.apply()
    }

    private fun savePreferencesToFirebase(affirmationPreferencesModel: AffirmationPreferencesModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users").child(userId).child("affirmationPreferences")
            .setValue(affirmationPreferencesModel)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        this@AffirmationSettingsActivity,
                        "Preferences saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()

                    editor.putBoolean("isAffirmationPreferences", true)
                    editor.apply()

                    databaseReference.child("Users").child(userId).child("affirmationPreferences")
                        .child("isAffirmationPreferences").setValue(true)

                } else {
                    Toast.makeText(
                        this@AffirmationSettingsActivity,
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
