package com.udacity.project4.authentication

import android.os.Bundle
import android.util.Log
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.udacity.project4.R

/**
 * This class should be the starting point of the app, It asks the users to sign in / register, and redirects the
 * signed in users to the RemindersActivity.
 */
class AuthenticationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_authentication)
        Log.d(TAG, "Activity was called")
        // TODO: Have the ReminderListFragment send the user to this activity if they are not logged in
//        val navController = findNavController(R.id.nav_host_fragment)
//        onBackPressedDispatcher.addCallback() {
//            navController.popBackStack(R.id.reminderListFragment, true)
//        }

//         TODO: Implement the create account and sign in using FirebaseUI, use sign in using email and sign in using Google

//          TODO: If the user was authenticated, send him to RemindersActivity

//          TODO: a bonus is to customize the sign in flow to look nice using :
        //https://github.com/firebase/FirebaseUI-Android/blob/master/auth/README.md#custom-layout

    }

    companion object {
        private const val TAG = "AuthenticationActivity"
    }
}
