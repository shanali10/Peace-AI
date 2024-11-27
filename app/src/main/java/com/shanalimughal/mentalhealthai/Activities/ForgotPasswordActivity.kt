package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.shanalimughal.mentalhealthai.databinding.ActivityForgotPasswordBinding


class ForgotPasswordActivity : AppCompatActivity() {
    lateinit var binding: ActivityForgotPasswordBinding

    private lateinit var progressDialog: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Sending email...")
        progressDialog.setCancelable(false)

        binding.goBackToLoginText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }

        binding.submitBtn.setOnClickListener {
            val email = binding.forgotPasswordEmail.text.toString()

            if (email.isEmpty()) {
                binding.forgotPasswordEmail.error = "Enter your email"
            } else {
                progressDialog.show()
                FirebaseAuth.getInstance().sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this,
                                "Password reset email has been sent to you!",
                                Toast.LENGTH_LONG
                            ).show()
                            binding.forgotPasswordEmail.text!!.clear()
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this,
                                "Some error occurred, try again",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
            }
        }
    }
}