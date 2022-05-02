package com.github.palFinderTeam.palfinder.navigation

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.github.palFinderTeam.palfinder.R
import com.github.palFinderTeam.palfinder.meetups.activities.SHOW_JOINED_ONLY
import com.github.palFinderTeam.palfinder.meetups.activities.ShowParam
import com.github.palFinderTeam.palfinder.profile.ProfileService
import com.github.palFinderTeam.palfinder.ui.login.LoginActivity
import com.github.palFinderTeam.palfinder.ui.settings.SettingsActivity
import com.github.palFinderTeam.palfinder.utils.createPopUp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainNavActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var bottomNavigationView: BottomNavigationView
    @Inject
    lateinit var profileService: ProfileService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_nav)

        auth = Firebase.auth

        val navController =
            (supportFragmentManager.findFragmentById(R.id.main_content) as NavHostFragment).navController
        bottomNavigationView = findViewById(R.id.bottom_nav)


        navController.addOnDestinationChangedListener { _, _, arguments ->
            // Hide navbar when needed
            hideShowNavBar(arguments?.getBoolean("ShowNavBar", false) == true)
        }

        // Make sure that selected item is the one displayed to the user
        navController.currentDestination?.let {
            when (it.id) {
                R.id.find_fragment -> bottomNavigationView.selectedItemId = R.id.nav_bar_find
                R.id.list_fragment -> bottomNavigationView.selectedItemId = R.id.nav_bar_groups
                R.id.creation_fragment -> bottomNavigationView.selectedItemId = R.id.nav_bar_create
            }
        }

        val animateLeftOptions = navOptions {
            this.anim {
                enter = R.anim.slide_in_right
                exit = R.anim.slide_out_left
            }
        }
        val animateRightOptions = navOptions {
            this.anim {
                enter = R.anim.slide_in_left
                exit = R.anim.slide_out_right
            }
        }

        // Bottom navigation behaviour
        bottomNavigationView.setOnItemSelectedListener { item ->
            val selected = bottomNavigationView.selectedItemId
            if (selected != item.itemId) {
                val direction = navItemToPosition(item.itemId) - navItemToPosition(selected)
                val options = if (direction < 0) animateRightOptions else animateLeftOptions


                when (item.itemId) {
                    R.id.nav_bar_create -> {
                        if (profileService.getLoggedInUserID() == null) {
                            createPopUp(
                                this,
                                { startActivity(Intent(this, LoginActivity::class.java)) },
                                textId = R.string.no_account_create,
                                continueButtonTextId = R.string.login
                            )
                            return@setOnItemSelectedListener false
                        } else {
                            navController.popBackStack()
                            navController.navigate(
                                R.id.creation_fragment,
                                args = null,
                                navOptions = options
                            )
                        }
                    }
                    R.id.nav_bar_groups -> {
                        if (profileService.getLoggedInUserID() == null) {
                            createPopUp(
                                this,
                                { startActivity(Intent(this, LoginActivity::class.java)) },
                                textId = R.string.no_account_groups,
                                continueButtonTextId = R.string.login
                            )
                            return@setOnItemSelectedListener false
                        } else {
                            navController.popBackStack()
                            val args = Bundle().apply {
                                putSerializable("showParam", ShowParam.ONLY_JOINED)
                            }
                            navController.navigate(
                                R.id.list_fragment,
                                args = args,
                                navOptions = options
                            )
                        }
                    }
                    R.id.nav_bar_find -> {
                        navController.popBackStack()
                        navController.navigate(
                            R.id.find_fragment,
                            args = null,
                            navOptions = options
                        )
                    }
                }
            }
            true
        }

    }

    fun hideShowNavBar(show: Boolean) {
        bottomNavigationView.isVisible = show
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miLogout -> {
                //Logout the user
                auth.signOut()
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken("341371843047-6i3a92lfmcb6555vsj9sb02tnhmkh4c8.apps.googleusercontent.com") //somehow cannot access value through google-service values.xml
                    .requestEmail()
                    .build()

                val client = GoogleSignIn.getClient(this, gso)
                client.signOut()
                val logoutIntent = Intent(this, LoginActivity::class.java)
                logoutIntent.flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(logoutIntent)
                return true
            }
            R.id.miSettings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            R.id.miUserSettings -> {
                return if (profileService.getLoggedInUserID() == null) {
                    createPopUp(
                        this,
                        { startActivity(Intent(this, LoginActivity::class.java)) },
                        textId = R.string.no_account_profile,
                        continueButtonTextId = R.string.login
                    )
                    true
                } else {
                    super.onOptionsItemSelected(item)
                }
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    private fun navItemToPosition(itemId: Int): Int {
        return when (itemId) {
            R.id.nav_bar_create -> 0
            R.id.nav_bar_find -> 1
            R.id.nav_bar_groups -> 2
            else -> -1
        }
    }

}