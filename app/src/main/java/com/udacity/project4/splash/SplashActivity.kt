package com.udacity.project4.splash

import androidx.appcompat.app.AppCompatActivity

import android.content.Intent
import android.os.Bundle

import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.authentication.AuthenticationActivity
import com.udacity.project4.locationreminders.RemindersActivity
import kotlinx.coroutines.*

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
    }

    override fun onStart() {
        super.onStart()
        navigate()
    }

    /**
     * Navigate to main activity or login activity according to authentication state, remove current activity from stack
     */
    private fun navigate() = GlobalScope.launch{
        delay(700)
        val intent = Intent(
            application,
            when (FirebaseAuth.getInstance().currentUser == null) {
                true -> AuthenticationActivity::class.java
                false -> RemindersActivity::class.java
            })
        intent.addFlags(Intent.FLAG_ACTIVITY_TASK_ON_HOME)
        startActivity(intent)
        finishAffinity()
    }
}