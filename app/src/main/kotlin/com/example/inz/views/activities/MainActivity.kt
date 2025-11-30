package com.example.inz.views.activities

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.inz.R
import com.example.inz.views.fragments.HistoryFragment
import com.example.inz.views.fragments.HomeFragment
import com.example.inz.views.fragments.StartWorkoutFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
//import com.google.firebase.perf.FirebasePerformance
//import com.google.firebase.perf.metrics.Trace

class MainActivity : AppCompatActivity() {
    var activeFragment: Fragment? = null

    lateinit var fragmentMap: MutableMap<Int, Fragment>
    lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val prefs = getSharedPreferences("WorkoutPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("isWorkoutInProgress", false).apply()
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        fragmentMap = HashMap()
        setupFragments()

        // Show the default fragment
        activeFragment = fragmentMap.get(R.id.home)
        supportFragmentManager.beginTransaction().show(activeFragment!!).commit()
        setupMenuEventHandler()
    }

    private fun setupFragments() {
        fragmentMap[R.id.home] = HomeFragment()
        fragmentMap[R.id.workout] = StartWorkoutFragment()
        fragmentMap[R.id.history] = HistoryFragment()

        for ((_, value) in fragmentMap) {
            supportFragmentManager.beginTransaction()
                    .add(R.id.nav_host_fragment, value)
                    .hide(value)
                    .commit()
        }
    }

    private fun setupMenuEventHandler() {
        bottomNavigationView.setOnItemSelectedListener { item: MenuItem ->
//            val trace: Trace = FirebasePerformance.getInstance().newTrace("fragmentSwitch")

//            trace.start()
            val selectedFragment = fragmentMap[item.itemId]

            if (selectedFragment != null && selectedFragment !== activeFragment) {
                supportFragmentManager.beginTransaction()
                        .hide(activeFragment!!)
                        .show(selectedFragment)
                        .commit()
                activeFragment = selectedFragment

//                trace.stop()
                return@setOnItemSelectedListener true
            }
            false
        }
    }

    fun goToHome() {
        bottomNavigationView.selectedItemId = R.id.home
    }

    fun goToWorkout() {
        bottomNavigationView.selectedItemId = R.id.workout
    }

    fun goToHitory() {
        bottomNavigationView.selectedItemId = R.id.history
    }

}
