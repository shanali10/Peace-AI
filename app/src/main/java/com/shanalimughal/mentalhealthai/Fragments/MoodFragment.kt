package com.shanalimughal.mentalhealthai.Fragments

import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentMoodBinding

class MoodFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentMoodBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var rgMood: RadioGroup

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    private lateinit var getUserMood: String

    private var newMood: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMoodBinding.inflate(inflater, container, false)

        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        rgMood = binding.rgMood

        rgMood.setOnCheckedChangeListener { group, checkedId ->
            newMood = group.findViewById<RadioButton>(checkedId)?.text.toString()
        }

        // initializing progress dialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Saving mood...")
        progressDialog.setCancelable(false)

        getUserMood = sharedPreferences.getString("mood", "") ?: ""

        when (getUserMood) {
            getString(R.string.happy) -> binding.rbHappy.isChecked = true
            getString(R.string.sad) -> binding.rbSad.isChecked = true
            getString(R.string.anxious) -> binding.rbAnxious.isChecked = true
            getString(R.string.stressed) -> binding.rbStressed.isChecked = true
            getString(R.string.calm) -> binding.rbCalm.isChecked = true
            getString(R.string.tired) -> binding.rbTired.isChecked = true
        }

        binding.btnSave.setOnClickListener {
            saveOnFirebase()
        }

        return binding.root
    }

    private fun saveOnFirebase() {
        progressDialog.show()
        val firebaseAuth = FirebaseAuth.getInstance()
        val databaseReference = FirebaseDatabase.getInstance().reference

        val userId = firebaseAuth.currentUser?.uid ?: return

        databaseReference.child("Users")
            .child(userId)
            .child("userPreferencesOnBoard")
            .child("mood")
            .setValue(newMood)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Mood Saved", Toast.LENGTH_SHORT).show()

                editor.putString("mood", newMood)
                editor.putBoolean("dailyMood", true)
                editor.apply()
                progressDialog.dismiss()
            }
            .addOnFailureListener {
                progressDialog.dismiss()
            }
    }

    override fun onPause() {
        super.onPause()
        editor.putBoolean("dailyMood", true)
        editor.apply()
    }
}
