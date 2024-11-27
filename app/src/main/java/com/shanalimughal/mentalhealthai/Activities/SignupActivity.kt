package com.shanalimughal.mentalhealthai.Activities

import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.shanalimughal.mentalhealthai.Models.UserModel
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivitySignupBinding

class SignupActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding

    private lateinit var auth: FirebaseAuth
    private lateinit var firebaseDatabse: FirebaseDatabase

    private lateinit var name: String
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var confirmPassword: String

    private lateinit var userModel: UserModel

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // variable initializations
        initailization()

        // on button click for account creation
        binding.createAccountBtn.setOnClickListener {
            createAccount()
        }

        // function for on-clicks on textviews
        textOnClicks()
    }

    fun initailization() {
        // initializing firebase authentication
        auth = Firebase.auth
        // initializing firebase database
        firebaseDatabse = FirebaseDatabase.getInstance()
        // initializing progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Creating Account...")
        progressDialog.setCancelable(false)

        // initializing shared preferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    fun createAccount() {
        name = binding.signupName.text.toString()
        email = binding.signupEmail.text.toString()
        password = binding.signupPassword.text.toString()
        confirmPassword = binding.confirmPassword.text.toString()

        if (name.isEmpty() || name.length < 3) {
            binding.signupName.error = "Please enter your name of minimum 3 characters"
        } else if (email.isEmpty()) {
            binding.signupEmail.error = "Please enter your email"
        } else if (password.isEmpty() || password.length < 6) {
            binding.signupPassword.error = "Please enter your password of minimum 6 characters"
        } else if (confirmPassword.isEmpty() || confirmPassword.length < 3) {
            binding.confirmPassword.error = "Please confirm your password"
        } else if (password != confirmPassword) {
            binding.confirmPassword.error = "Password does not match"
        } else {
            // showing progress dialog
            progressDialog.show()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {

                        auth.currentUser?.sendEmailVerification()
                            ?.addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    // dismissing progress dialog
                                    progressDialog.dismiss()

                                    userModel = UserModel(name, email)
                                    val user = auth.currentUser
                                    firebaseDatabse.reference.child("Users").child(user!!.uid)
                                        .setValue(userModel)

                                    // saving user details to shared preferences
                                    editor.putString("name", name)
                                    editor.putString("email", email)
                                    editor.apply()

                                    // opening verify email activity
                                    val intent = Intent(this, VerifyEmailActivity::class.java)
                                    intent.putExtra("email", email)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Toast.makeText(
                                        this,
                                        "Failed to send verification email",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    progressDialog.dismiss()
                                }
                            }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Authentication failed.\nCheck your email or password",
                            Toast.LENGTH_SHORT,
                        ).show()

                        // dismissing progress dialog
                        progressDialog.dismiss()
                    }
                }
        }
    }

    fun textOnClicks() {
        binding.alreadyHaveAccountText.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
        }
    }
}