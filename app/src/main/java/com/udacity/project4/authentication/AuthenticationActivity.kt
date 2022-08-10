package com.udacity.project4.authentication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.udacity.project4.R
import com.udacity.project4.databinding.ActivityAuthenticationBinding
import com.udacity.project4.locationreminders.RemindersActivity

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AuthenticationActivity"
    }

    private lateinit var binding: ActivityAuthenticationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthenticationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d(TAG, "Activity called")

        binding.loginButton.setOnClickListener { launchSignIn() }

    }

    private fun launchSignIn() {
        // Give users the option to sign in / register with their email or Google account. If users
        // choose to register with their email, they will need to create a password as well.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )

        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setTheme(R.style.AppTheme)
            .build()
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        // val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            // val user = FirebaseAuth.getInstance().currentUser
            // Log.i(TAG, "Successfully signed in user ${user}!")

            // navigate to the reminders activity on successful login
            val intent = Intent(this, RemindersActivity::class.java)
            startActivity(intent)

            // close out this activity
            this.finish()
        } else {
            // Sign in failed.
            //if (response == null) {
            Snackbar.make(binding.authCoordinatorLayout, R.string.sign_in_failed, Snackbar.LENGTH_LONG)
                .show()
            //}

        }
    }
}
