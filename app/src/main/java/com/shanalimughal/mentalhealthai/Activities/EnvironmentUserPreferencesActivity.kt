package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.NatureAndEnvironmentModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityEnvironmentUserPreferencesBinding

class EnvironmentUserPreferencesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEnvironmentUserPreferencesBinding
    private lateinit var environmentModel: NatureAndEnvironmentModel

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var rgEngagementFrequency: RadioGroup
    private lateinit var rgNatureImportance: RadioGroup
    private lateinit var cbRecyclingWasteReduction: CheckBox
    private lateinit var cbEnergyConservation: CheckBox
    private lateinit var cbSustainableLiving: CheckBox
    private lateinit var cbClimateChangeAwareness: CheckBox
    private lateinit var cbWildlifeConservation: CheckBox
    private lateinit var cbOutdoorActivities: CheckBox
    private lateinit var rgInformationPreference: RadioGroup
    private lateinit var submitBtn: Button

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    // getting info from shared preferences
    private lateinit var getEngagementFrequency: String
    private lateinit var getInterests: MutableSet<String>
    private lateinit var getNatureImportance: String
    private lateinit var getInformationPref: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEnvironmentUserPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializingVariables()
        getUserPreferences()

        submitBtn.setOnClickListener {
            progressDialog.show()
            savePreferences()
        }
    }

    private fun initializingVariables() {
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        // initializing layout views
        rgEngagementFrequency = binding.rgEngagementFrequency
        rgNatureImportance = binding.rgNatureImportance
        cbRecyclingWasteReduction = binding.cbRecyclingWasteReduction
        cbEnergyConservation = binding.cbEnergyConservation
        cbSustainableLiving = binding.cbSustainableLiving
        cbClimateChangeAwareness = binding.cbClimateChangeAwareness
        cbWildlifeConservation = binding.cbWildlifeConservation
        cbOutdoorActivities = binding.cbOutdoorActivities
        rgInformationPreference = binding.rgInformationPreference
        submitBtn = binding.submitButton

        // disabling the save button in the start
        binding.submitButton.isEnabled = false

        // set listeners
        rgEngagementFrequency.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgNatureImportance.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgInformationPreference.setOnCheckedChangeListener { _, _ -> validateInputs() }

        val checkBoxes = listOf(
            cbRecyclingWasteReduction,
            cbEnergyConservation,
            cbSustainableLiving,
            cbClimateChangeAwareness,
            cbWildlifeConservation,
            cbOutdoorActivities,
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
        getEngagementFrequency =
            sharedPreferences.getString("natureEngagementFrequency", "").toString()
        getInterests =
            sharedPreferences.getStringSet("natureInterestedAreas", emptySet())?.toMutableSet()
                ?: mutableSetOf()
        getNatureImportance =
            sharedPreferences.getString("natureImportance", "").toString()
        getInformationPref =
            sharedPreferences.getString("naturePreferredInformation", "").toString()

        // displaying user preferences
        displayUserPreferences()
    }


    private fun displayUserPreferences() {
        // Setting radio buttons for engagement frequency
        when (getEngagementFrequency) {
            getString(R.string.daily) -> binding.rbDaily.isChecked = true
            getString(R.string.weekly) -> binding.rbWeekly.isChecked = true
            getString(R.string.monthly) -> binding.rbMonthly.isChecked = true
            getString(R.string.rarely) -> binding.rbRarely.isChecked = true
        }

        // Setting radio buttons for nature importance
        when (getNatureImportance) {
            getString(R.string.very_important) -> binding.rbVeryImportant.isChecked = true
            getString(R.string.somewhat_important) -> binding.rbSomewhatImportant.isChecked = true
            getString(R.string.not_very_important) -> binding.rbNotVeryImportant.isChecked = true
            getString(R.string.not_important) -> binding.rbNotImportant.isChecked = true
        }

        // Setting radio buttons for engagement frequency
        when (getInformationPref) {
            getString(R.string.articles) -> binding.rbArticles.isChecked = true
            getString(R.string.stories) -> binding.rbStories.isChecked = true
            getString(R.string.short_novels) -> binding.rbShortNovels.isChecked = true
        }

        // Set checkboxes for meditation goals
        cbRecyclingWasteReduction.isChecked =
            getInterests.contains(cbRecyclingWasteReduction.text.toString())
        cbEnergyConservation.isChecked =
            getInterests.contains(cbEnergyConservation.text.toString())
        cbSustainableLiving.isChecked = getInterests.contains(cbSustainableLiving.text.toString())
        cbClimateChangeAwareness.isChecked =
            getInterests.contains(cbClimateChangeAwareness.text.toString())
        cbWildlifeConservation.isChecked =
            getInterests.contains(cbWildlifeConservation.text.toString())
        cbOutdoorActivities.isChecked =
            getInterests.contains(cbOutdoorActivities.text.toString())

    }

    private fun savePreferences() {
        val rgEngagementFrequency =
            findViewById<RadioButton>(rgEngagementFrequency.checkedRadioButtonId)?.text.toString()
        val rgNatureImportance =
            findViewById<RadioButton>(rgNatureImportance.checkedRadioButtonId)?.text.toString()
        val natureInterest = mutableListOf<String>().apply {
            if (cbRecyclingWasteReduction.isChecked) add(cbRecyclingWasteReduction.text.toString())
            if (cbEnergyConservation.isChecked) add(cbEnergyConservation.text.toString())
            if (cbSustainableLiving.isChecked) add(cbSustainableLiving.text.toString())
            if (cbClimateChangeAwareness.isChecked) add(cbClimateChangeAwareness.text.toString())
            if (cbWildlifeConservation.isChecked) add(cbWildlifeConservation.text.toString())
            if (cbOutdoorActivities.isChecked) add(cbOutdoorActivities.text.toString())
        }
        val rgInformationPreference =
            findViewById<RadioButton>(rgInformationPreference.checkedRadioButtonId)?.text.toString()

        environmentModel = NatureAndEnvironmentModel(
            rgEngagementFrequency, rgNatureImportance, natureInterest, rgInformationPreference
        )

        savePreferencesToSharedPreferences(environmentModel)
        savePreferencesToFirebase(environmentModel)
    }

    private fun validateInputs() {
        val isEngagementFrequencySelected = rgEngagementFrequency.checkedRadioButtonId != -1
        val isNatureImportanceSelected = rgNatureImportance.checkedRadioButtonId != -1
        val isNatureInterestSelected = listOf(
            cbRecyclingWasteReduction,
            cbEnergyConservation,
            cbSustainableLiving,
            cbClimateChangeAwareness,
            cbWildlifeConservation,
            cbOutdoorActivities
        ).any { it.isChecked }

        val isInformationPreferenceSelected = rgInformationPreference.checkedRadioButtonId != -1

        submitBtn.isEnabled =
            isEngagementFrequencySelected && isNatureImportanceSelected && isInformationPreferenceSelected && isNatureInterestSelected
    }

    private fun savePreferencesToSharedPreferences(natureModel: NatureAndEnvironmentModel) {
        editor.putString("natureEngagementFrequency", natureModel.engagementFrequency)
        editor.putString("natureImportance", natureModel.importance)
        editor.putStringSet("natureInterestedAreas", natureModel.interestedAreas.toSet())
        editor.putString("naturePreferredInformation", natureModel.preferredInformation)
        editor.apply()
    }

    private fun savePreferencesToFirebase(natureModel: NatureAndEnvironmentModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users")
            .child(userId)
            .child("natureAndEnvironmentPreferences")
            .setValue(natureModel)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(this, "Preferences saved successfully", Toast.LENGTH_SHORT)
                        .show()
                    progressDialog.dismiss()

                    editor.putBoolean("isNaturePreferences", true)
                    editor.apply()

                    databaseReference.child("Users")
                        .child(userId)
                        .child("natureAndEnvironmentPreferences")
                        .child("isNaturePreferences")
                        .setValue(true)

//                     going to main Environment Activity now
                    val intent = Intent(this, EnvironmentActivity::class.java)
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
}