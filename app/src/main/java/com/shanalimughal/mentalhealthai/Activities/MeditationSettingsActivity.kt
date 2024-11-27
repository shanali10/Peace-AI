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
import com.shanalimughal.mentalhealthai.Models.MeditationPreferencesModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivityMeditationSettingsBinding

class MeditationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMeditationSettingsBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var rgFrequency: RadioGroup
    private lateinit var cbStressRelief: CheckBox
    private lateinit var cbAnxietyReduction: CheckBox
    private lateinit var cbMindfulness: CheckBox
    private lateinit var cbEmotionalBalance: CheckBox
    private lateinit var rgPreferredTime: RadioGroup
    private lateinit var rgTime: RadioGroup
    private lateinit var cbGuidedMeditation: CheckBox
    private lateinit var cbBreathingExercises: CheckBox
    private lateinit var cbBodyScanMeditation: CheckBox
    private lateinit var cbLovingKindnessMeditation: CheckBox
    private lateinit var btnSave: Button

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    // getting info from shared preferences
    private lateinit var getOftenMeditate: String
    private lateinit var getMeditationGoal: MutableSet<String>
    private lateinit var getPreferredMeditationTime: String
    private lateinit var getPreferredMeditationSessionLength: String
    private lateinit var getPreferredMeditationTypes: MutableSet<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeditationSettingsBinding.inflate(layoutInflater)
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
        rgFrequency = binding.rgFrequency
        cbStressRelief = binding.cbStressRelief
        cbAnxietyReduction = binding.cbAnxietyReduction
        cbMindfulness = binding.cbMindfulness
        cbEmotionalBalance = binding.cbEmotionalBalance
        rgPreferredTime = binding.rgPreferredTime
        rgTime = binding.rgTime
        cbGuidedMeditation = binding.cbGuidedMeditation
        cbBreathingExercises = binding.cbBreathingExercises
        cbBodyScanMeditation = binding.cbBodyScanMeditation
        cbLovingKindnessMeditation = binding.cbLovingKindnessMeditation
        btnSave = binding.btnSave

        // disabling the save button in the start
        binding.btnSave.isEnabled = false

        // Set listeners
        rgFrequency.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgPreferredTime.setOnCheckedChangeListener { _, _ -> validateInputs() }
        rgTime.setOnCheckedChangeListener { _, _ -> validateInputs() }

        val checkBoxes = listOf(
            cbStressRelief,
            cbAnxietyReduction,
            cbMindfulness,
            cbEmotionalBalance,
            cbGuidedMeditation,
            cbBreathingExercises,
            cbBodyScanMeditation,
            cbLovingKindnessMeditation
        )
        checkBoxes.forEach { checkBox ->
            checkBox.setOnCheckedChangeListener { _, _ -> validateInputs() }
        }

        // initializing progress dialog
        progressDialog = ProgressDialog(this@MeditationSettingsActivity)
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
    }

    private fun getUserPreferences() {
        getOftenMeditate = sharedPreferences.getString("oftenMeditate", "") ?: ""
        getMeditationGoal =
            sharedPreferences.getStringSet("meditationGoal", emptySet())?.toMutableSet()
                ?: mutableSetOf()
        getPreferredMeditationTime =
            sharedPreferences.getString("preferredMeditationTime", "") ?: ""
        getPreferredMeditationSessionLength =
            sharedPreferences.getString("preferredMeditationSessionLength", "") ?: ""
        getPreferredMeditationTypes =
            sharedPreferences.getStringSet("preferredMeditationTypes", emptySet())?.toMutableSet()
                ?: mutableSetOf()


        // displaying user preferences
        displayUserPreferences()
    }

    private fun displayUserPreferences() {
        // Set radio buttons for frequency
        when (getOftenMeditate) {
            getString(R.string.dailyMeditate) -> binding.rbDaily.isChecked = true
            getString(R.string.twice_a_day_meditate) -> binding.rbTwiceDaily.isChecked = true
            getString(R.string.weeklyMeditate) -> binding.rbWeekly.isChecked = true
            getString(R.string.neverMeditate) -> binding.rbNever.isChecked = true
        }

        // Set checkboxes for meditation goals
        cbStressRelief.isChecked = getMeditationGoal.contains(cbStressRelief.text.toString())
        cbAnxietyReduction.isChecked =
            getMeditationGoal.contains(cbAnxietyReduction.text.toString())
        cbMindfulness.isChecked = getMeditationGoal.contains(cbMindfulness.text.toString())
        cbEmotionalBalance.isChecked =
            getMeditationGoal.contains(cbEmotionalBalance.text.toString())

        // Set radio buttons for preferred meditation time
        when (getPreferredMeditationTime) {
            getString(R.string.morning) -> binding.rbMorning.isChecked = true
            getString(R.string.afternoon) -> binding.rbAfternoon.isChecked = true
            getString(R.string.evening) -> binding.rbEvening.isChecked = true
        }

        // Set radio buttons for session length
        when (getPreferredMeditationSessionLength) {
            getString(R.string._5_minutes) -> binding.rb5Minutes.isChecked = true
            getString(R.string._10_minutes) -> binding.rb10Minutes.isChecked = true
            getString(R.string._15_minutes) -> binding.rb15Minutes.isChecked = true
            getString(R.string._20_minutes) -> binding.rb20Minutes.isChecked = true
        }

        // Set checkboxes for meditation types
        cbGuidedMeditation.isChecked =
            getPreferredMeditationTypes.contains(cbGuidedMeditation.text.toString())
        cbBreathingExercises.isChecked =
            getPreferredMeditationTypes.contains(cbBreathingExercises.text.toString())
        cbBodyScanMeditation.isChecked =
            getPreferredMeditationTypes.contains(cbBodyScanMeditation.text.toString())
        cbLovingKindnessMeditation.isChecked =
            getPreferredMeditationTypes.contains(cbLovingKindnessMeditation.text.toString())
    }

    private fun savePreferences() {
        // Get selected radio button values
        val selectedFrequencyValue =
            binding.root.findViewById<RadioButton>(rgFrequency.checkedRadioButtonId).text.toString()
        val selectedPreferredTimeValue =
            binding.root.findViewById<RadioButton>(rgPreferredTime.checkedRadioButtonId).text.toString()
        val selectedSessionLengthValue =
            binding.root.findViewById<RadioButton>(rgTime.checkedRadioButtonId).text.toString()

        // Get checked checkbox values for meditation goals
        val meditationGoals = mutableListOf<String>().apply {
            if (binding.cbStressRelief.isChecked) add(binding.cbStressRelief.text.toString())
            if (binding.cbAnxietyReduction.isChecked) add(binding.cbAnxietyReduction.text.toString())
            if (binding.cbMindfulness.isChecked) add(binding.cbMindfulness.text.toString())
            if (binding.cbEmotionalBalance.isChecked) add(binding.cbEmotionalBalance.text.toString())
        }

        // Get checked checkbox values for meditation types
        val meditationType = mutableListOf<String>().apply {
            if (binding.cbGuidedMeditation.isChecked) add(binding.cbGuidedMeditation.text.toString())
            if (binding.cbBreathingExercises.isChecked) add(binding.cbBreathingExercises.text.toString())
            if (binding.cbBodyScanMeditation.isChecked) add(binding.cbBodyScanMeditation.text.toString())
            if (binding.cbLovingKindnessMeditation.isChecked) add(binding.cbLovingKindnessMeditation.text.toString())
        }

        // Create preferences model
        val mediationPreferenceModel = MeditationPreferencesModel(
            selectedFrequencyValue,
            meditationGoals,
            selectedPreferredTimeValue,
            selectedSessionLengthValue,
            meditationType
        )

        // Save preferences to SharedPreferences and Firebase
        savePreferencesToSharedPreferences(mediationPreferenceModel)
        savePreferencesToFirebase(mediationPreferenceModel)
    }

    private fun validateInputs() {
        val isOftenMediateSelected = rgFrequency.checkedRadioButtonId != -1
        val isMediationGoalSelected = listOf(
            cbStressRelief, cbAnxietyReduction, cbMindfulness, cbEmotionalBalance
        ).any { it.isChecked }
        val isMediationPreferredTimeSelected = rgPreferredTime.checkedRadioButtonId != -1
        val isMediationSessionSelected = rgTime.checkedRadioButtonId != -1
        val isPreferredMediationTypeSelected = listOf(
            cbGuidedMeditation,
            cbBreathingExercises,
            cbBodyScanMeditation,
            cbLovingKindnessMeditation
        ).any { it.isChecked }

        btnSave.isEnabled =
            isOftenMediateSelected && isMediationGoalSelected && isMediationPreferredTimeSelected && isMediationSessionSelected && isPreferredMediationTypeSelected
    }

    private fun savePreferencesToSharedPreferences(mediationPreferenceModel: MeditationPreferencesModel) {
        editor.putString("oftenMeditate", mediationPreferenceModel.oftenMeditate)
        editor.putStringSet("meditationGoal", mediationPreferenceModel.meditationGoal.toSet())
        editor.putString(
            "preferredMeditationTime", mediationPreferenceModel.preferredMeditationTime
        )
        editor.putString(
            "preferredMeditationSessionLength",
            mediationPreferenceModel.preferredMeditationSessionLength
        )
        editor.putStringSet(
            "preferredMeditationTypes", mediationPreferenceModel.preferredMeditationTypes.toSet()
        )
        editor.apply()
    }

    private fun savePreferencesToFirebase(mediationPreferenceModel: MeditationPreferencesModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users").child(userId).child("meditationPreferences")
            .setValue(mediationPreferenceModel).addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        this@MeditationSettingsActivity,
                        "Preferences saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()

                    editor.putBoolean("isMeditationPreferences", true)
                    editor.apply()

                    databaseReference.child("Users").child(userId).child("meditationPreferences")
                        .child("isMeditationPreferences").setValue(true)

                } else {
                    Toast.makeText(
                        this@MeditationSettingsActivity,
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
