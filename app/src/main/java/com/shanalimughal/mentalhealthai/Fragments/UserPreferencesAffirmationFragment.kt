package com.shanalimughal.mentalhealthai.Fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.shanalimughal.mentalhealthai.Models.AffirmationPreferencesModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentUserPreferencesAffirmationBinding

class UserPreferencesAffirmationFragment : Fragment() {

    private lateinit var binding: FragmentUserPreferencesAffirmationBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentUserPreferencesAffirmationBinding.inflate(inflater, container, false)

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
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving preferences...")
        progressDialog.setCancelable(false)
    }

    private fun savePreferences() {
        // Get selected radio button values
        val selectedPreferredAffirmationValue = binding.root.findViewById<RadioButton>(rgStyle.checkedRadioButtonId).text.toString()

        // Get checked checkbox values for meditation goals
        val affrimationTheme = mutableListOf<String>().apply {
            if (binding.cbMotivation.isChecked) add(binding.cbMotivation.text.toString())
            if (binding.cbPositivity.isChecked) add(binding.cbPositivity.text.toString())
            if (binding.cbSelfEsteem.isChecked) add(binding.cbSelfEsteem.text.toString())
            if (binding.cbRelaxation.isChecked) add(binding.cbRelaxation.text.toString())
        }

        // Create preferences model
         val affirmationPreferencesModel = AffirmationPreferencesModel(
            affrimationTheme, selectedPreferredAffirmationValue)

        // Save preferences to SharedPreferences and Firebase
        savePreferencesToSharedPreferences(affirmationPreferencesModel)
        savePreferencesToFirebase(affirmationPreferencesModel)
    }

    private val inputWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            validateInputs()
        }
    }

    private fun validateInputs() {
        val isAffirmationStyleSeleted = rgStyle.checkedRadioButtonId != -1
        val isAffirmationThemeSelected = listOf(cbMotivation, cbRelaxation, cbSelfEsteem, cbPositivity).any { it.isChecked }

        btnSave.isEnabled = isAffirmationStyleSeleted && isAffirmationThemeSelected
    }

    private fun savePreferencesToSharedPreferences(affirmationPreferencesModel: AffirmationPreferencesModel) {
        editor.putStringSet("affirmationTheme", affirmationPreferencesModel.affirmationTheme.toSet())
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
                    Toast.makeText(requireContext(), "Preferences saved successfully", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()

                    editor.putBoolean("isAffirmationPreferences", true)
                    editor.apply()

                    databaseReference.child("Users").child(userId).child("affirmationPreferences").child("isAffirmationPreferences").setValue(true)

                    val fragmentTransaction = requireActivity().supportFragmentManager.beginTransaction()
                    fragmentTransaction.replace(R.id.linearLayout, AffirmationFragment())
                    fragmentTransaction.commit()

                } else {
                    Toast.makeText(requireContext(), "Failed to save preferences, try again", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                }
            }
    }
}