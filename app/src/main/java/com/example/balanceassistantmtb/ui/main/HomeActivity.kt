package com.example.balanceassistantmtb

import android.app.AlertDialog
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.example.balanceassistantmtb.ui.main.DashboardFragment
import com.example.balanceassistantmtb.ui.main.HomeFragment

class HomeActivity : AppCompatActivity() {

    private lateinit var homeFragment: HomeFragment
    private lateinit var dashboardFragment: DashboardFragment
    private var fragmentPos = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get the fragment to return to from extras 1:group, 2:feed, 0:profile
        val fragment = intent.extras?.get("fragment")
        val bottomNavigation: BottomNavigationView = findViewById(R.id.btm_nav)

        // display and set checked fragment in BottomNav
        when (fragment) {
            "1" -> {
                dashboardFragment = DashboardFragment()
                fragmentPos = 1
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, dashboardFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
                bottomNavigation.menu.findItem(R.id.dashboard).isChecked =true
            }
            else -> {
                homeFragment = HomeFragment()
                fragmentPos = 0
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
                bottomNavigation.menu.findItem(R.id.home).isChecked = true

            }
        }

        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    homeFragment =
                        HomeFragment()
                    fragmentPos = 0
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, homeFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.dashboard -> {
                    dashboardFragment =
                        DashboardFragment()
                    fragmentPos = 1
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, dashboardFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
            }
            true
        }
    }


    /**
     * close the app, when user is in HomeTab and press the android device back button. User gets
     * ask via alert dialog if he want to exit the app
     * */
    override fun onBackPressed() {
        if(fragmentPos == 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.exit)
            builder.setMessage(R.string.exit_message)
            builder.setPositiveButton(R.string.yes) { _, _ ->
                finishAffinity()
            }
            builder.setNegativeButton(R.string.no) { _, _ ->
            }
            builder.show()
        }
    }
}