package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.geofence.GeofencingConstants.ACTION_GEOFENCE_EVENT
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SaveReminderFragment : BaseFragment() {

    //Get the view model this time as a single to be shared with the another fragment
    // override val _viewModel by inject<SaveReminderViewModel>()
    override val _viewModel by sharedViewModel<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding
    private var geofencingClient: GeofencingClient? = null
    private var reminder: ReminderDataItem? = null

    private val runningQOrLater: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else PendingIntent.FLAG_UPDATE_CURRENT
        PendingIntent.getBroadcast(context, 1, intent, flags)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSaveReminderBinding.inflate(inflater, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel
        Log.d(TAG, "Location string is ${_viewModel.reminderSelectedLocationStr.value}")

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            reminder = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
            )
            if (!_viewModel.validateEnteredData(reminder!!)) return@setOnClickListener

            checkPermissionsAndSaveReminder()
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceAndSaveReminder() {
        val reminderDataItem = reminder!!
        if (geofencingClient == null) geofencingClient =
            LocationServices.getGeofencingClient(requireActivity())
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .setNotificationResponsiveness(GeofencingConstants.GEOFENCE_NOTIFICATION_RESPONSIVENESS)
            .build()

        val geofenceRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

        // remove any previous geofence associated with this reminder and add in a new one
        geofencingClient?.removeGeofences(mutableListOf(reminderDataItem.id))?.run {
            addOnCompleteListener {
                geofencingClient?.addGeofences(geofenceRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Log.d(TAG, "Geofence added for ${geofence.requestId}")
                        _viewModel.validateAndSaveReminder(reminderDataItem)
                    }
                    addOnFailureListener {
                        Log.d(TAG, "Couldn't add geofence for ${geofence.requestId}")
                        Snackbar.make(
                            binding.saveReminderCoordinatorLayout,
                            R.string.error_adding_geofence,
                            Snackbar.LENGTH_INDEFINITE
                        ).show()
                    }
                }
            }
        }
    }

    // useful for testing.
    fun removeAllGeofences() {
        if (geofencingClient == null) geofencingClient =
            LocationServices.getGeofencingClient(requireActivity())
        geofencingClient?.removeGeofences(geofencePendingIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy called")
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    /**
     * Starts the permission check and saves the reminder and starts the geofence
     */
    private fun checkPermissionsAndSaveReminder() {
        if (foregroundAndBackgroundLocationPermissionApproved()) {
            checkDeviceLocationSettingsAndSaveReminder()
        } else {
            requestForegroundAndBackgroundLocationPermissions()
        }
    }

    /*
     *  When we get the result from asking the user to turn on device location, we call
     *  checkDeviceLocationSettingsAndSaveReminder again to make sure it's actually on, but
     *  we don't resolve the check to keep the user from seeing an endless loop.
    */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            checkDeviceLocationSettingsAndSaveReminder(false)
        }
    }


    /*
     * In all cases, we need to have the location permission.  On Android 10+ (Q) we need to have
     * the background permission as well.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionResult")
        if (
            grantResults.isEmpty() ||
            grantResults[LOCATION_PERMISSION_INDEX] == PackageManager.PERMISSION_DENIED ||
            (requestCode == REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE &&
                    grantResults[BACKGROUND_LOCATION_PERMISSION_INDEX] ==
                    PackageManager.PERMISSION_DENIED)
        ) {
            Snackbar.make(
                binding.saveReminderCoordinatorLayout,
                R.string.precise_location_permission_denied,
                Snackbar.LENGTH_INDEFINITE
            )
                .setAction(R.string.settings) {
                    startActivity(Intent().apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    })
                }.show()
        } else {
            checkDeviceLocationSettingsAndSaveReminder()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }


    /*
     *  Uses the Location Client to check the current state of location settings, and gives the user
     *  the opportunity to turn on location services within our app.
     */
    private fun checkDeviceLocationSettingsAndSaveReminder(resolve: Boolean = true) {
        val locationRequest = LocationRequest.create().apply {
            priority = Priority.PRIORITY_LOW_POWER
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask =
            settingsClient.checkLocationSettings(builder.build())
        locationSettingsResponseTask.addOnFailureListener { exception ->
            if (exception is ResolvableApiException && resolve) {
                try {
                    startIntentSenderForResult(
                        exception.resolution.intentSender,
                        REQUEST_TURN_DEVICE_LOCATION_ON,
                        null,
                        0,
                        0,
                        0,
                        null
                    )
                    /*
                    // does not work in fragment
                    exception.startResolutionForResult(
                        requireActivity(),
                        REQUEST_TURN_DEVICE_LOCATION_ON
                    )
                     */
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.d(TAG, "Error getting location settings resolution: " + sendEx.message)
                }
            } else {
                Snackbar.make(
                    binding.saveReminderCoordinatorLayout,
                    R.string.location_required_error, Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.ok) {
                    checkDeviceLocationSettingsAndSaveReminder()
                }.show()
            }
        }
        locationSettingsResponseTask.addOnCompleteListener {
            if (it.isSuccessful) {
                Log.d(TAG, "calling addGeofenceAndSaveReminder")
                addGeofenceAndSaveReminder()
            } else {
                Log.d(TAG, "not calling addGeofenceAndSaveReminder")
            }
        }

    }

    /*
     *  Determines whether the app has the appropriate permissions across Android 10+ and all other
     *  Android versions.
     */
    @SuppressLint("InlinedApi")
    private fun foregroundAndBackgroundLocationPermissionApproved(): Boolean {
        val foregroundLocationApproved = (
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ))
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                PackageManager.PERMISSION_GRANTED ==
                        ActivityCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        )
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    /*
     *  Requests ACCESS_FINE_LOCATION and (on Android 10+ (Q) ACCESS_BACKGROUND_LOCATION.
     */
    @SuppressLint("InlinedApi")
    private fun requestForegroundAndBackgroundLocationPermissions() {
        if (foregroundAndBackgroundLocationPermissionApproved())
            return
        var permissionsArray = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        val resultCode = when {
            runningQOrLater -> {
                permissionsArray += Manifest.permission.ACCESS_BACKGROUND_LOCATION
                REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE
            }
            else -> REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
        }
        Log.d(TAG, "Request foreground only location permission")
        requestPermissions(permissionsArray, resultCode)

    }

}

private const val REQUEST_FOREGROUND_AND_BACKGROUND_PERMISSION_RESULT_CODE = 33
private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
private const val REQUEST_TURN_DEVICE_LOCATION_ON = 29
private const val LOCATION_PERMISSION_INDEX = 0
private const val BACKGROUND_LOCATION_PERMISSION_INDEX = 1
private const val TAG = "SaveReminderFragment"