package com.shanalimughal.mentalhealthai.Activities

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.ActivitySignInBinding

class SignInActivity : AppCompatActivity() {
    lateinit var binding: ActivitySignInBinding

    private lateinit var auth: FirebaseAuth

    private lateinit var email: String
    private lateinit var password: String

    // progress dialog
    private lateinit var progressDialog: ProgressDialog

    private lateinit var googleSignInClient: GoogleSignInClient

    private val webClientId: String =
        "579885620078-m9bfdft3kkmpjqir8srni1nlv5ohrmne.apps.googleusercontent.com"

    private lateinit var sharedPreferences: SharedPreferences

    private var isUserPreferencesSaved: Boolean? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // variable initialization
        initialization()

        // onTextViewClicks
        onTextViewClicks()

        // signing in user
        binding.signInBtn.setOnClickListener {
            signInWithCustomEmailandPassword()
        }
        binding.googleSignInBtn.setOnClickListener {
            signInWithGoogle()
        }
    }

    fun initialization() {
        // initializing shared preferences
        sharedPreferences = getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        isUserPreferencesSaved = sharedPreferences.getBoolean("userPreferencesOnBoard", false)

        // initializing firebase authentication
        auth = Firebase.auth
        // initializing progress dialog
        progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Please wait...")
        progressDialog.setCancelable(false)

        // initializing google sign in options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        // initializing google sign in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    fun signInWithCustomEmailandPassword() {
        email = binding.signinEmail.text.toString()
        password = binding.signinPassword.text.toString()

        if (email.isEmpty()) {
            binding.signinEmail.error = "Please enter your email"
        } else if (password.isEmpty()) {
            binding.signinPassword.error = "Please enter your password"
        } else {
            // showing progress dialog
            progressDialog.show()

            // signing in user
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, updating UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")

                        if (auth.currentUser!!.isEmailVerified) {
                            FirebaseDatabase.getInstance().getReference("Users")
                                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                                .child("userPreferencesOnBoard")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            // dismissing progress dialog
                                            progressDialog.dismiss()

                                            isUserPreferencesSaved =
                                                snapshot.child("isUserPreferencesOnBoard")
                                                    .getValue(Boolean::class.java)

                                            if (!isUserPreferencesSaved!!) {
                                                val intent = Intent(
                                                    this@SignInActivity,
                                                    UserPreferencesActivity::class.java
                                                )
                                                startActivity(intent)
                                                finish()
                                                Toast.makeText(
                                                    this@SignInActivity,
                                                    "Signed In Successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@SignInActivity,
                                                    "Signed In Successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                startActivity(
                                                    Intent(
                                                        this@SignInActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                        } else {
                                            progressDialog.dismiss()
                                            Toast.makeText(
                                                this@SignInActivity,
                                                "Signed In Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(
                                                Intent(
                                                    this@SignInActivity,
                                                    UserPreferencesActivity::class.java
                                                )
                                            )
                                            finish()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        progressDialog.dismiss()
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                                        Toast.makeText(
                                            baseContext,
                                            "Authentication failed.\nCheck your email or password",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                })
                        } else {
                            progressDialog.dismiss()
                            Toast.makeText(
                                this@SignInActivity,
                                "Please verify your email",
                                Toast.LENGTH_SHORT
                            ).show()
                            val intent = Intent(this, VerifyEmailActivity::class.java)
                            intent.putExtra("email", email)
                            startActivity(intent)
                        }
                    } else {
                        // dismissing progress dialog
                        progressDialog.dismiss()
                        Toast.makeText(
                            baseContext,
                            "Network error, try again",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        }
    }

    fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        launcher.launch(signInIntent)
    }

    private val launcher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleResults(task)
            } else {
                Toast.makeText(this, "Some error occurred, try again", Toast.LENGTH_SHORT).show()
            }
        }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        progressDialog.setMessage("Signing In...")
        progressDialog.show()

        if (task.isSuccessful) {
            val account: GoogleSignInAccount? = task.result
            if (account != null) {
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if (authTask.isSuccessful) {
                        val firebaseUser = auth.currentUser
                        if (firebaseUser != null) {
                            // Save user details to Firebase Realtime Database
                            val user = mapOf(
                                "uid" to firebaseUser.uid,
                                "name" to firebaseUser.displayName,
                                "email" to firebaseUser.email,
                                "profileImage" to (firebaseUser.photoUrl?.toString() ?: "")
                            )

                            FirebaseDatabase.getInstance().getReference("Users")
                                .child(firebaseUser.uid)
                                .child("userPreferencesOnBoard")
                                .addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        if (snapshot.exists()) {
                                            // dismissing progress dialog
                                            progressDialog.dismiss()
                                            isUserPreferencesSaved =
                                                snapshot.child("isUserPreferencesOnBoard")
                                                    .getValue(Boolean::class.java)

                                            if (!isUserPreferencesSaved!!) {
                                                val intent = Intent(
                                                    this@SignInActivity,
                                                    UserPreferencesActivity::class.java
                                                )
                                                startActivity(intent)
                                                finish()
                                                Toast.makeText(
                                                    this@SignInActivity,
                                                    "Signed In Successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@SignInActivity,
                                                    "Signed In Successfully",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                                startActivity(
                                                    Intent(
                                                        this@SignInActivity,
                                                        MainActivity::class.java
                                                    )
                                                )
                                                finish()
                                            }
                                        } else {
                                            FirebaseDatabase.getInstance().getReference("Users")
                                                .child(firebaseUser.uid)
                                                .setValue(user)

                                            Toast.makeText(
                                                this@SignInActivity,
                                                "Signed In Successfully",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            startActivity(
                                                Intent(
                                                    this@SignInActivity,
                                                    UserPreferencesActivity::class.java
                                                )
                                            )
                                            finish()
                                            finish()
                                        }
                                    }

                                    override fun onCancelled(error: DatabaseError) {
                                        progressDialog.dismiss()
                                        // If sign in fails, display a message to the user.
                                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                                        Toast.makeText(
                                            baseContext,
                                            "Authentication failed.\nCheck your email or password",
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                        progressDialog.dismiss()
                                    }
                                })
                        }
                    } else {
                        Toast.makeText(this, authTask.exception.toString(), Toast.LENGTH_SHORT)
                            .show()
                        progressDialog.dismiss()
                    }
                }
            }
        } else {
            Toast.makeText(this, task.exception.toString(), Toast.LENGTH_SHORT).show()
            progressDialog.dismiss()
        }
    }

    fun onTextViewClicks() {
        binding.createNewAccountText.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
        binding.forgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }
}
