package com.shanalimughal.mentalhealthai.Fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.shanalimughal.mentalhealthai.Activities.MeditationDetailActivity
import com.shanalimughal.mentalhealthai.Activities.TherapySessionActivity
import com.shanalimughal.mentalhealthai.Activities.TherapySummaryDetailActivity
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.databinding.FragmentHomeBinding
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@SuppressLint("SetTextI18n", "DefaultLocale")
class HomeFragment : Fragment() {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private lateinit var userName: String
    private lateinit var profession: String

    // personal preferences
    private lateinit var getUserMood: String
    private lateinit var getUserPersonalGoals: MutableSet<String>
    private var getUserSleepHours: Int? = 0
    private lateinit var getExceriseFrequency: String

    // meditation preferences
    private lateinit var getOftenMeditate: String
    private lateinit var getMeditationGoal: MutableSet<String>
    private lateinit var getPreferredMeditationTime: String
    private lateinit var getPreferredMeditationSessionLength: String
    private lateinit var getPreferredMeditationTypes: MutableSet<String>


    private lateinit var meditationResponses: MutableSet<String>

    // affirmation preferences
    private lateinit var getAffirmationTheme: MutableSet<String>
    private lateinit var getPreferredAffirmation: String

    // nature preferences
    private lateinit var getNatureIterestAreas: MutableSet<String>

    private lateinit var profileImageUrl: String

    private lateinit var imageUri: Uri
    private val PICK_IMAGE_REQUEST = 1

    private lateinit var progressDialog: ProgressDialog

    private lateinit var lastMeditationDate: String
    private lateinit var lastMeditation: String
    private var meditationProgress: Float = 0.0f

    private lateinit var therapySummary: String
    private lateinit var therapyDate: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false)

        sharedPreferences = requireContext().getSharedPreferences("userPrefs", Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()

        userName = sharedPreferences.getString("userName", "").toString()
        profession = sharedPreferences.getString("profession", "").toString()

        therapySummary = sharedPreferences.getString("summary", "").toString()
        therapyDate = sharedPreferences.getString("therapyDate", "").toString()

        // personal preferences
        getUserMood = sharedPreferences.getString("mood", "") ?: ""
        getUserPersonalGoals =
            sharedPreferences.getStringSet("goals", emptySet())?.toMutableSet() ?: mutableSetOf()
        getUserSleepHours = sharedPreferences.getInt("sleepHours", 0)
        getExceriseFrequency = sharedPreferences.getString("exerciseFrequency", "") ?: ""

        // meditation preferences

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

        meditationResponses =
            sharedPreferences.getStringSet("meditationResponses", mutableSetOf())!!

        // affirmation preferences
        getAffirmationTheme =
            sharedPreferences.getStringSet("affirmationTheme", emptySet())?.toMutableSet()
                ?: mutableSetOf()
        getPreferredAffirmation = sharedPreferences.getString("preferredAffirmation", "") ?: ""

        // nature preferences
        getNatureIterestAreas =
            sharedPreferences.getStringSet("natureInterestedAreas", emptySet())?.toMutableSet()
                ?: mutableSetOf()

        lastMeditationDate = sharedPreferences.getString("lastMeditationDate", "").toString()
        lastMeditation = sharedPreferences.getString("lastMeditation", "").toString()
        meditationProgress = sharedPreferences.getFloat(lastMeditation, 0f)

        // progress dialog
        progressDialog = ProgressDialog(requireContext())
        progressDialog.setMessage("Loading...")
        progressDialog.setCancelable(false)

        if (lastMeditationDate == "") {
            binding.lastMeditationDate.text = "You haven't started Meditating yet"
        } else {
            binding.lastMeditationDate.text = getRelativeTimeAgo(lastMeditationDate)
        }

        if (lastMeditation == "") {
            binding.lastMeditationCardView.visibility = View.GONE
        } else {
            binding.lastMeditationCardView.visibility = View.VISIBLE

            var updatedMeditation =
                lastMeditation.replace("*", "").replace("#", "").replace("\n", " ")
                    .take(150) + "..."

            if (updatedMeditation[0].toString() == " ") {
                updatedMeditation = lastMeditation.replace("*", "")
                    .replaceFirst(" ", "")
                    .replace("#", "")
                    .replace("\n", " ")
                    .take(150) + "..."
            }

            binding.meditationTitle.text = updatedMeditation

            binding.meditationGoalsLastMeditation.text = "Meditation Goals: ${getMeditationGoal}"
            binding.sleepHoursLastMeditation.text = "Sleep Hours: $getUserSleepHours"
            binding.meditationDuration.text =
                "Meditation Duration: $getPreferredMeditationSessionLength"

            val formattedValue = String.format("%.2f", meditationProgress)
            binding.progress.text = "Last Time Progress: $formattedValue%"
            binding.progressBar.progress = meditationProgress.toInt()
        }

        getLastMeditation()

        binding.viewDetails.setOnClickListener {
            lastMeditation = sharedPreferences.getString("lastMeditation", "").toString()
            val intent = Intent(context, MeditationDetailActivity::class.java)
            intent.putExtra("meditationDescription", lastMeditation)
            startActivity(intent)
        }

        profileImageUrl = sharedPreferences.getString("profileImageUrl", "").toString()
        if (profileImageUrl.isNotEmpty()) {
            Picasso.get().load(profileImageUrl).placeholder(R.drawable.profile_placeholder)
                .into(binding.profileImage)
        }

        if (userName == "") {
            getUserDetails()
        } else {
            val firstName = userName.split(" ")[0]
            binding.userName.text = "Hello, $firstName"
        }
        binding.userProfession.text = "($profession)"

        settingPreferences()

        binding.addProfileButton.setOnClickListener {
            openFileChooser()
        }

        val updatedResponse =
            therapySummary.replace("*", "").replace("#", "").replace("\n", " ").take(100) + "..."

        if (therapySummary == "") {
            binding.therapySummary.text =
                "Join a therapy session with our advanced AI Therapist to enhance your mental well-being."
            binding.therapyTime.text = "Start your first Therapy Session"
        } else {
            binding.aiTherapistTitle.text = "Last Session Details"
            binding.therapySummary.text = updatedResponse
            binding.startTherapy.text = "View Details"
            binding.therapyTime.text = "Session Time: ${getRelativeTimeAgo(therapyDate)}"
        }

        binding.startTherapy.setOnClickListener {

            if (binding.startTherapy.text == "View Details") {
                val intent = Intent(context, TherapySummaryDetailActivity::class.java)
                intent.putExtra("summary", therapySummary)
                startActivity(intent)
            } else {
                val intent = Intent(context, TherapySessionActivity::class.java)
                startActivity(intent)
            }
        }

        return binding.root
    }

    private fun getUserDetails() {
        progressDialog.show()
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("name").value.toString()
                    val email = snapshot.child("email").value.toString()
                    val isFirstTimeAffirmationsGenerated =
                        snapshot.child("isFirstTimeAffirmationsGenerated").value as Boolean
                    val isFirstTimeMeditationsGenerated =
                        snapshot.child("isFirstTimeMeditationsGenerated").value as Boolean
                    val isFirstTimeNatureAndEnvironmentGenerated =
                        snapshot.child("isFirstTimeNatureAndEnvironmentGenerated").value as Boolean
                    val profileImage = snapshot.child("profileImage").value.toString()

                    editor.putString("userName", userName)
                    editor.putString("email", email)
                    editor.putString("profileImageUrl", profileImage)
                    editor.putBoolean(
                        "isFirstTimeAffirmationsGenerated", isFirstTimeAffirmationsGenerated
                    )
                    editor.putBoolean(
                        "isFirstTimeMeditationsGenerated", isFirstTimeMeditationsGenerated
                    )
                    editor.putBoolean(
                        "isFirstTimeNatureAndEnvironmentGenerated",
                        isFirstTimeNatureAndEnvironmentGenerated
                    )

                    val firstName = userName.split(" ")[0]
                    binding.userName.text = "Hello, ${firstName}"

                    Picasso.get().load(profileImage).placeholder(R.drawable.profile_placeholder)
                        .into(binding.profileImage)

                    getPersonalPreferences()
                } else {
                    getPersonalPreferences()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
            }

        })
    }

    private fun getPersonalPreferences() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("userPreferencesOnBoard")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val profession = snapshot.child("profession").value.toString()
                        val exerciseFrequency = snapshot.child("exerciseFrequency").value.toString()
                        val isUserPreferencesOnBoard =
                            snapshot.child("isUserPreferencesOnBoard").value as Boolean
                        val mood = snapshot.child("mood").value.toString()
                        val sleepHours = snapshot.child("sleepHours").value.toString().toInt()

                        for (items in snapshot.child("goals").children) {
                            getUserPersonalGoals.add(items.value.toString())
                            editor.putStringSet("goals", getUserPersonalGoals)
                            editor.apply()
                        }

                        editor.putString("profession", profession)
                        editor.putString("exerciseFrequency", exerciseFrequency)
                        editor.putBoolean(
                            "isUserPreferencesOnBoard", isUserPreferencesOnBoard
                        )
                        editor.putString("mood", mood)
                        editor.putInt("sleepHours", sleepHours)
                        editor.apply()

                        binding.userProfession.text = "($profession)"
                        binding.personalGoals.text = "Personal Goals: $getUserPersonalGoals"
                        binding.mood.text = "Mood: $mood"
                        binding.sleepHours.text = "Sleep Hours: $sleepHours"
                        binding.excerciseFrequency.text = "Exercise Frequency: $exerciseFrequency"

                        gettingMeditationPreferences()
                    } else {
                        gettingMeditationPreferences()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                }

            })
    }

    private fun gettingMeditationPreferences() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("meditationPreferences")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val oftenMeditate = snapshot.child("oftenMeditate").value.toString()
                        val preferredMeditationSessionLength =
                            snapshot.child("preferredMeditationSessionLength").value.toString()
                        val isMeditationPreferences =
                            snapshot.child("isMeditationPreferences").value as Boolean
                        val preferredMeditationTime =
                            snapshot.child("preferredMeditationTime").value.toString()

                        for (items in snapshot.child("meditationGoal").children) {
                            getMeditationGoal.add(items.value.toString())
                            editor.putStringSet("meditationGoal", getMeditationGoal)
                            editor.apply()
                        }
                        for (items in snapshot.child("preferredMeditationTypes").children) {
                            getPreferredMeditationTypes.add(items.value.toString())
                            editor.putStringSet(
                                "preferredMeditationTypes", getPreferredMeditationTypes
                            )
                            editor.apply()
                        }

                        editor.putString("oftenMeditate", oftenMeditate)
                        editor.putString(
                            "preferredMeditationSessionLength", preferredMeditationSessionLength
                        )
                        editor.putBoolean(
                            "isMeditationPreferences", isMeditationPreferences
                        )
                        editor.putString("preferredMeditationTime", preferredMeditationTime)
                        editor.apply()
                        getMeditationResponses()
                    } else {
                        getMeditationResponses()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                }

            })
    }

    private fun getMeditationResponses() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("meditationResponses").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (items in snapshot.children) {
                        meditationResponses.add(items.value.toString())
                        editor.putStringSet("meditationResponses", meditationResponses)
                        editor.apply()
                    }
                    getAffirmationPreferences()
                } else {
                    getAffirmationPreferences()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
            }

        })
    }

    private fun getAffirmationPreferences() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("affirmationPreferences")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val preferredAffirmation =
                            snapshot.child("preferredAffirmation").value.toString()
                        val isAffirmationPreferences =
                            snapshot.child("isAffirmationPreferences").value as Boolean

                        for (items in snapshot.child("affirmationTheme").children) {
                            getAffirmationTheme.add(items.value.toString())
                            editor.putStringSet("affirmationTheme", getAffirmationTheme)
                            editor.apply()
                        }

                        editor.putString("preferredAffirmation", preferredAffirmation)
                        editor.putBoolean(
                            "isAffirmationPreferences", isAffirmationPreferences
                        )
                        editor.apply()
                        getNaturePreferences()
                    } else {
                        getNaturePreferences()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                }

            })
    }

    private fun getNaturePreferences() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("natureAndEnvironmentPreferences")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val engagementFrequency =
                            snapshot.child("engagementFrequency").value.toString()
                        val importance = snapshot.child("importance").value.toString()
                        val preferredInformation =
                            snapshot.child("preferredInformation").value.toString()
                        val isNaturePreferences =
                            snapshot.child("isNaturePreferences").value as Boolean

                        for (items in snapshot.child("interestedAreas").children) {
                            getNatureIterestAreas.add(items.value.toString())
                            editor.putStringSet("natureInterestedAreas", getNatureIterestAreas)
                            editor.apply()
                        }

                        editor.putString("natureEngagementFrequency", engagementFrequency)
                        editor.putString("natureImportance", importance)
                        editor.putString("naturePreferredInformation", preferredInformation)
                        editor.putBoolean(
                            "isNaturePreferences", isNaturePreferences
                        )
                        editor.apply()
                        lastTherapySessionDetaisl()
                    } else {
                        lastTherapySessionDetaisl()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                }
            })
    }


    private fun lastTherapySessionDetaisl() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
        ref.child("therapySessions").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (items in snapshot.children) {
                        therapySummary = items.child("summary").value.toString()
                        therapyDate = items.child("date").value.toString()

                        editor.putString("summary", therapySummary)
                        editor.putString("therapyDate", therapyDate)
                        editor.apply()

                        val updatedResponse =
                            therapySummary.replace("*", "").replace("#", "").replace("\n", " ")
                                .take(100) + "..."

                        if (therapySummary == "") {
                            binding.therapySummary.text =
                                "Join a therapy session with our advanced AI Therapist to enhance your mental well-being."
                            binding.therapyTime.text = "Start your first Therapy Session"
                        } else {
                            binding.aiTherapistTitle.text = "Last Session Details"
                            binding.therapySummary.text = updatedResponse
                            binding.startTherapy.text = "View Details"
                            binding.therapyTime.text =
                                "Session Time: ${getRelativeTimeAgo(therapyDate)}"
                        }
                    }
                    getLastMeditation()
                } else {
                    getLastMeditation()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                progressDialog.dismiss()
            }
        })
    }

    private fun getLastMeditation() {
        val ref = FirebaseDatabase.getInstance().getReference("Users")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        ref.child("LastMeditationDetails")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {

                        lastMeditation = snapshot.child("lastMeditation").value.toString()
                        lastMeditationDate = snapshot.child("lastMeditationDate").value.toString()
                        meditationProgress = sharedPreferences.getFloat(lastMeditation, 0f)
                        getUserSleepHours = sharedPreferences.getInt("sleepHours", 0)

                        getPreferredMeditationSessionLength =
                            sharedPreferences.getString("preferredMeditationSessionLength", "")
                                ?: ""

                        if (lastMeditationDate == "") {
                            binding.lastMeditationDate.text = "You haven't started Meditating yet"
                        } else {
                            binding.lastMeditationDate.text = getRelativeTimeAgo(lastMeditationDate)
                        }

                        if (lastMeditation == "") {
                            binding.lastMeditationCardView.visibility = View.GONE
                        } else {
                            binding.lastMeditationCardView.visibility = View.VISIBLE

                            var updatedMeditation =
                                lastMeditation.replace("*", "").replace("#", "").replace("\n", " ")
                                    .take(150) + "..."

                            if (updatedMeditation[0].toString() == " ") {
                                updatedMeditation = lastMeditation.replace("*", "")
                                    .replaceFirst(" ", "")
                                    .replace("#", "")
                                    .replace("\n", " ")
                                    .take(150) + "..."
                            }

                            binding.meditationTitle.text = updatedMeditation

                            binding.meditationGoalsLastMeditation.text =
                                "Meditation Goals: ${getMeditationGoal}"
                            binding.sleepHoursLastMeditation.text =
                                "Sleep Hours: $getUserSleepHours"
                            binding.meditationDuration.text =
                                "Meditation Duration: $getPreferredMeditationSessionLength"

                            val formattedValue = String.format("%.2f", meditationProgress)
                            binding.progress.text = "Last Time Progress: $formattedValue%"
                            binding.progressBar.progress = meditationProgress.toInt()
                        }

                        editor.putString("lastMeditation", lastMeditation)
                        editor.putString("lastMeditationDate", lastMeditationDate)
                        editor.apply()

                        progressDialog.dismiss()
                    } else {
                        progressDialog.dismiss()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    progressDialog.dismiss()
                }

            })
    }

    private fun settingPreferences() {
        binding.personalGoals.text = "Personal Goals: $getUserPersonalGoals"
        binding.mood.text = "Today Mood: $getUserMood"
        binding.sleepHours.text = "Sleep Hours: $getUserSleepHours"
        binding.excerciseFrequency.text = "Exercise Frequency: $getExceriseFrequency"
    }

    private fun openFileChooser() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data!!
            Picasso.get().load(imageUri).into(binding.profileImage)
            uploadImageToFirebase()
        }
    }

    private fun uploadImageToFirebase() {
        progressDialog.setMessage("Uploading...")
        progressDialog.show()
        val fileReference = FirebaseStorage.getInstance().getReference("uploads")
            .child(System.currentTimeMillis().toString() + ".jpg")

        fileReference.putFile(imageUri).addOnSuccessListener {
            fileReference.downloadUrl.addOnSuccessListener { uri ->
                val imageUrl = uri.toString()
                saveImageInfoToDatabase(imageUrl)
            }
        }.addOnFailureListener {
            Toast.makeText(
                requireContext(), "Upload failed: ${it.message}", Toast.LENGTH_SHORT
            ).show()

            progressDialog.dismiss()
        }
    }

    private fun saveImageInfoToDatabase(imageUrl: String) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        databaseReference.child(FirebaseAuth.getInstance().currentUser!!.uid).child("profileImage")
            .setValue(imageUrl).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    editor.putString("profileImageUrl", imageUrl)
                    editor.apply()
                    Toast.makeText(requireContext(), "Upload successful", Toast.LENGTH_SHORT).show()
                    progressDialog.dismiss()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Upload failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()

                    progressDialog.dismiss()
                }
            }
    }

    fun getRelativeTimeAgo(dateString: String): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date: Date = dateFormat.parse(dateString) ?: return "unknown time"

        val now = Date()
        val diffInMillies = now.time - date.time
        val diffInSeconds = TimeUnit.MILLISECONDS.toSeconds(diffInMillies)
        val diffInMinutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillies)
        val diffInHours = TimeUnit.MILLISECONDS.toHours(diffInMillies)
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillies)

        return when {
            diffInSeconds < 60 -> "just now"
            diffInMinutes < 60 -> "$diffInMinutes minute${if (diffInMinutes > 1) "s" else ""} ago"
            diffInHours < 24 -> "$diffInHours hour${if (diffInHours > 1) "s" else ""} ago"
            diffInDays < 7 -> "$diffInDays day${if (diffInDays > 1) "s" else ""} ago"
            diffInDays < 30 -> "${diffInDays / 7} week${if ((diffInDays / 7) > 1) "s" else ""} ago"
            diffInDays < 365 -> "${diffInDays / 30} month${if ((diffInDays / 30) > 1) "s" else ""} ago"
            else -> "${diffInDays / 365} year${if ((diffInDays / 365) > 1) "s" else ""} ago"
        }
    }
}