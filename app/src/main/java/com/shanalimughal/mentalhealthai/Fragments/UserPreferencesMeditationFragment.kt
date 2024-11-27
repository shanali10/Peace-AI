package com.shanalimughal.mentalhealthai.Fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.Models.MeditationPreferencesModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentUserPreferencesMeditationBinding

class UserPreferencesMeditationFragment : Fragment() {
    private lateinit var binding: FragmentUserPreferencesMeditationBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserPreferencesMeditationBinding.inflate(inflater, container, false)

        // initializing the variables
        initializingVariables()

        // onclick listener on saving preferences
        binding.btnSave.setOnClickListener {
            progressDialog.show()
            savePreferences()
        }

        return binding.root
    }

    private fun initializingVariables() {
        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
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
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
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
//        val isProfessionFilled = etProfession.text.isNotBlank()
        val isOftenMediateSelected = rgFrequency.checkedRadioButtonId != -1
        val isMediationGoalSelected = listOf(
            cbStressRelief,
            cbAnxietyReduction,
            cbMindfulness,
            cbEmotionalBalance
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
            "preferredMeditationTime",
            mediationPreferenceModel.preferredMeditationTime
        )
        editor.putString(
            "preferredMeditationSessionLength",
            mediationPreferenceModel.preferredMeditationSessionLength
        )
        editor.putStringSet(
            "preferredMeditationTypes",
            mediationPreferenceModel.preferredMeditationTypes.toSet()
        )
        editor.apply()
    }

    private fun savePreferencesToFirebase(mediationPreferenceModel: MeditationPreferencesModel) {
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users").child(userId).child("meditationPreferences")
            .setValue(mediationPreferenceModel)
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    Toast.makeText(
                        requireContext(),
                        "Preferences saved successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()

                    editor.putBoolean("isMeditationPreferences", true)
                    editor.apply()

                    databaseReference.child("Users").child(userId).child("meditationPreferences")
                        .child("isMeditationPreferences").setValue(true)

                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.linearLayout, MeditationFragment())
                    fragmentTransaction.commit()

                } else {
                    Toast.makeText(
                        requireContext(),
                        "Failed to save preferences, try again",
                        Toast.LENGTH_SHORT
                    ).show()
                    progressDialog.dismiss()
                }
            }
    }
}
