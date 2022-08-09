package com.udacity.project4.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.base.BaseRecyclerViewAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private const val EXTENSIONS_TAG = "Extensions"

/**
 * Extension function to setup the RecyclerView
 */
fun <T> RecyclerView.setup(
    adapter: BaseRecyclerViewAdapter<T>
) {
    this.apply {
        layoutManager = LinearLayoutManager(this.context)
        this.adapter = adapter
    }
}

fun Fragment.setTitle(title: String) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.title = title
    }
}

fun Fragment.setDisplayHomeAsUpEnabled(bool: Boolean) {
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(
            bool
        )
    }
}

//animate changing the view visibility
fun View.fadeIn() {
    this.visibility = View.VISIBLE
    this.alpha = 0f
    this.animate().alpha(1f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeIn.alpha = 1f
        }
    })
}

//animate changing the view visibility
fun View.fadeOut() {
    this.animate().alpha(0f).setListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            this@fadeOut.alpha = 1f
            this@fadeOut.visibility = View.GONE
        }
    })
}

/* -- following section coming from https://github.com/android/permissions-samples -- */
// https://github.com/android/permissions-samples/blob/main/RuntimePermissionsBasicKotlin/Application/src/main/java/com/example/android/basicpermissions/util/ViewExt.kt
fun View.showSnackbar(msgId: Int, length: Int) {
    showSnackbar(context.getString(msgId), length)
}

fun View.showSnackbar(msg: String, length: Int) {
    showSnackbar(msg, length, null) {}
}

fun View.showSnackbar(
    msgId: Int,
    length: Int,
    actionMessageId: Int,
    action: (View) -> Unit
) {
    showSnackbar(context.getString(msgId), length, context.getString(actionMessageId), action)
}

fun View.showSnackbar(
    msg: String,
    length: Int,
    actionMessage: CharSequence?,
    action: (View) -> Unit
) {
    val snackbar = Snackbar.make(this, msg, length)
    if (actionMessage != null) {
        snackbar.setAction(actionMessage) {
            action(this)
        }.show()
    }
}

/* -- end section coming from https://github.com/android/permissions-samples -- */

// returns true if one of the permissions from the permissions array argument returns true for
// shouldShowRequestPermissionRationale
@RequiresApi(Build.VERSION_CODES.M)
fun Fragment.shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean =
    permissions.all {
        val result = shouldShowRequestPermissionRationale(it)
        Log.d(EXTENSIONS_TAG,"shouldShowRequestPermissionRationale for $it: $result")
        result
    }

// returns true if all of the permissions from the permissions array are granted
fun Fragment.hasPermissions(permissions: Array<String>): Boolean =
    permissions.all {
        ActivityCompat.checkSelfPermission(
            requireContext(),
            it
        ) == PackageManager.PERMISSION_GRANTED
    }
