package com.shanalimughal.mentalhealthai.Activities

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.shanalimughal.mentalhealthai.BackgroundTasks.DailySchedulerClass
import com.shanalimughal.mentalhealthai.Fragments.*
import com.shanalimughal.mentalhealthai.R
import com.shanalimughal.mentalhealthai.RoomDB.ChatDatabase
import com.shanalimughal.mentalhealthai.RoomDB.ChatMessageDao
import com.shanalimughal.mentalhealthai.databinding.ActivityMainBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    private var isMeditationPreferences: Boolean = false
    private var isAffirmationPreferences: Boolean = false
    private var isNaturePreferences: Boolean = false

    private var dailyMood: Boolean = false

    private lateinit var db: ChatDatabase
    private lateinit var dao: ChatMessageDao


    private lateinit var googleSignInClient: GoogleSignInClient

    private val webClientId: String =
        "250261175694-p6ikrpo66r82i9c68b7sj0kiuqncj1e3.apps.googleusercontent.com"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

//         setting a background task for daily mood check in
        DailySchedulerClass.scheduleDailyReset(this)


        // initializing google sign in options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()

        // initializing google sign in client
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        db = ChatDatabase.getDatabase(this)
        dao = db.chatMessageDao()

        drawerLayout = binding.drawerLayout

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        binding.navView.setNavigationItemSelectedListener(this)

        // initializing shared preferences
        sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE)
        editor = sharedPreferences.edit()

        isMeditationPreferences = sharedPreferences.getBoolean("isMeditationPreferences", false)
        isAffirmationPreferences = sharedPreferences.getBoolean("isAffirmationPreferences", false)
        isNaturePreferences = sharedPreferences.getBoolean("isNaturePreferences", false)

        dailyMood = sharedPreferences.getBoolean("dailyMood", false)

        binding.selectedItemText.text = resources.getString(R.string.home)
        replaceFragment(HomeFragment())

        if (!dailyMood) {
            val bottomSheet = MoodFragment()
            bottomSheet.show(supportFragmentManager, bottomSheet.tag)
        }

        binding.bottomBar.onItemSelected = {
            when (it) {
                0 -> {
                    binding.selectedItemText.text = resources.getString(R.string.home)
                    replaceFragment(HomeFragment())
                }

                1 -> {
                    binding.selectedItemText.text = resources.getString(R.string.mediation)
                    val isMeditationPreferences =
                        sharedPreferences.getBoolean("isMeditationPreferences", false)
                    if (isMeditationPreferences) {
                        replaceFragment(MeditationFragment())
                    } else {
                        replaceFragment(UserPreferencesMeditationFragment())
                    }
                }

                2 -> {
                    binding.selectedItemText.text = "AI Chatbot"
                    replaceFragment(ChatbotFragment())
                }

                3 -> {
                    binding.selectedItemText.text = resources.getString(R.string.affirmation)
                    val isAffirmationPreferences =
                        sharedPreferences.getBoolean("isAffirmationPreferences", false)
                    if (isAffirmationPreferences) {
                        replaceFragment(AffirmationFragment())
                    } else {
                        replaceFragment(UserPreferencesAffirmationFragment())
                    }
                }
            }
        }

        binding.moreIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.linearLayout, fragment)
        fragmentTransaction.commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.therapySession -> {
                startActivity(Intent(this, TherapySessionActivity::class.java))
                return true
            }

            R.id.natureAndEnvironment -> {
                val isNaturePreferences = sharedPreferences.getBoolean("isNaturePreferences", false)
                if (isNaturePreferences) {
                    startActivity(Intent(this, EnvironmentActivity::class.java))
                } else {
                    startActivity(Intent(this, EnvironmentUserPreferencesActivity::class.java))
                }
                return true
            }

            R.id.community -> {
                startActivity(Intent(this, CommunityForumActivity::class.java))
                return true
            }

            R.id.therapyHistory -> {
                startActivity(Intent(this, TherapySessionHistoryActivity::class.java))
                return true
            }

            R.id.favoriteAffirmations -> {
                startActivity(Intent(this, FavoriteAffirmationsActivity::class.java))
                return true
            }

            R.id.favoriteMeditations -> {
                startActivity(Intent(this, FavoriteMeditationsActivity::class.java))
                return true
            }

            R.id.personalInfoSettings -> {
                startActivity(Intent(this, PersonalInfoSettingsActivity::class.java))
                return true
            }

            R.id.affirmationSettings -> {
                startActivity(Intent(this, AffirmationSettingsActivity::class.java))
                return true
            }

            R.id.meditationSettings -> {
                startActivity(Intent(this, MeditationSettingsActivity::class.java))
                return true
            }

            R.id.logOut -> {
                val builder = AlertDialog.Builder(this)
                builder.setMessage("Are you sure you want to logout?")
                    .setCancelable(false)
                    .setPositiveButton("Yes") { dialog, id ->
                        logOut()
                    }
                    .setNegativeButton("No") { dialog, id ->
                        dialog.dismiss()
                    }
                val alert = builder.create()
                alert.show()
                return true
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun logOut() {
        editor.remove("isMeditationPreferences")
        editor.remove("isAffirmationPreferences")
        editor.remove("dailyMood")
        editor.remove("affirmationTheme")
        editor.remove("preferredAffirmation")
        editor.remove("userName")
        editor.remove("name")
        editor.remove("profileImageUrl")
        editor.remove("affirmationResponses")
        editor.remove("favoriteAffirmations")
        editor.remove("meditationGoal")
        editor.remove("preferredMeditationSessionLength")
        editor.remove("preferredMeditationTypes")
        editor.remove("profession")
        editor.remove("mood")
        editor.remove("goals")
        editor.remove("sleepHours")
        editor.remove("meditationResponses")
        editor.remove("lastMeditationDate")
        editor.remove("lastMeditation")
        editor.remove("oftenMeditate")
        editor.remove("preferredMeditationTime")
        editor.remove("isMeditationPreferences")
        editor.remove("exerciseFrequency")
        editor.remove("userPreferencesOnBoard")
        editor.remove("email")
        editor.remove("aiQuestions")
        editor.remove("isFirstTimeAffirmationsGenerated")
        editor.remove("summary")
        editor.remove("therapyDate")
        editor.remove("lastMeditationDate")
        editor.remove("lastMeditation")
        editor.remove("isFirstTimeMeditationsGenerated")
        editor.remove("sessionHistory")
        editor.remove("feedback")
        editor.remove("isNaturePreferences")
        editor.remove("natureEngagementFrequency")
        editor.remove("natureImportance")
        editor.remove("natureInterestedAreas")
        editor.remove("naturePreferredInformation")
        editor.remove("natureResponses")
        editor.remove("isFirstTimeNatureAndEnvironmentGenerated")
        editor.remove("isUserPreferencesSaved")
        editor.remove("isChatFetchedFromFirebase")
        editor.apply()

        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut()
        startActivity(Intent(this, SignInActivity::class.java))
        finish()

        // Delete all Room database
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteAllMessages()
        }

        Toast.makeText(this, "Logout Successfully", Toast.LENGTH_SHORT).show()
    }
}
