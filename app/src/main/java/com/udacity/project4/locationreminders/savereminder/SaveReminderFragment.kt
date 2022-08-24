package com.udacity.project4.locationreminders.savereminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.geofence.GeofencingConstants
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {

    companion object {
        private val TAG = SaveReminderFragment::class.java.simpleName
    }

    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel by inject<SaveReminderViewModel>()
    private lateinit var binding: FragmentSaveReminderBinding
    private var geofencingClient: GeofencingClient? = null

    // A PendingIntent for the Broadcast Receiver that handles geofence transitions.
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), GeofenceBroadcastReceiver::class.java)
        intent.action = GeofencingConstants.ACTION_GEOFENCE_EVENT
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else PendingIntent.FLAG_UPDATE_CURRENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, flags)
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
            val title = _viewModel.reminderTitle.value
            val description = _viewModel.reminderDescription.value
            val location = _viewModel.reminderSelectedLocationStr.value
            val latitude = _viewModel.latitude.value
            val longitude = _viewModel.longitude.value

            // TODO: move validation to the view model layer
            if (title.isNullOrBlank()) {
                binding.reminderTitle.apply {
                    error = getString(R.string.err_enter_title)
                    hint = getString(R.string.select_title)
                }
                return@setOnClickListener
            }
            if (location.isNullOrBlank() || latitude == null || longitude == null) {
                binding.selectLocation.error = getString(R.string.err_select_location)
                Snackbar.make(
                    binding.saveReminderCoordinatorLayout,
                    R.string.err_select_location,
                    Snackbar.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            val reminder = ReminderDataItem(title,description,location,latitude,longitude)

            addGeofenceAndSaveReminder(reminder)
        }
    }

    @SuppressLint("MissingPermission")
    private fun addGeofenceAndSaveReminder(reminderDataItem: ReminderDataItem) {
        if (geofencingClient == null) geofencingClient =
            LocationServices.getGeofencingClient(requireActivity())
        val geofence = Geofence.Builder()
            .setRequestId(reminderDataItem.id)
            .setCircularRegion(
                reminderDataItem.latitude!!,
                reminderDataItem.longitude!!,
                GeofencingConstants.GEOFENCE_RADIUS_IN_METERS
            )
            .setExpirationDuration(GeofencingConstants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val geofenceRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()

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

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }
}
