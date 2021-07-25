package com.udacity.project4.authentication

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.reminderslist.ReminderListFragment

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
    }


    fun startAuthFlow(view: View) {
        val authProviders = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build())
        val authIntent = AuthUI.getInstance()
            .createSignInIntentBuilder().setIsSmartLockEnabled(false)
            .setAvailableProviders(authProviders)
            .build()
        startActivityForResult(authIntent, AUTH_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AUTH_REQUEST_CODE) {
            val result = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                navigateToRemindersActivity()
            } else {
                Toast.makeText(this, "Authentication failed with code ${result?.error?.errorCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToRemindersActivity() {
        startActivity(Intent(application, RemindersActivity::class.java))
        if (this.isTaskRoot) finishAffinity() // if not root, no need to call it because it will start RemindersActivity, which is SingleTask
    }

    private companion object {
        const val AUTH_REQUEST_CODE = 1901
    }

    override fun onBackPressed() {
        finishAffinity()
    }
}
