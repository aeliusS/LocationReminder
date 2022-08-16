package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.hasPermissions
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.shouldShowRequestPermissionRationale
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

const val PERMISSION_REQUEST_LOCATION = 0

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "SelectLocationFragment"
        private val permissionsArray = if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        private val backgroundPermissionArray =
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                arrayOf()
            }
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSelectLocationBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSelectLocationBinding.inflate(inflater, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner

        // add in the menu options
        val menuHost: MenuHost = requireActivity()
        setupMenuOptions(menuHost)

        setDisplayHomeAsUpEnabled(true)

        // TODO: add the map setup implementation
        // Get the SupportMapFragment and request notification when the map is ready to be used.
        // val mapFragment = requireActivity().supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        // mapFragment?.getMapAsync(this)

        // request runtime permissions if necessary; API level 23 (M) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestForegroundLocationPermissions()
        }
        // TODO: zoom to the user location after taking his permission

        // TODO: add style to the map
        // TODO: put a marker to location that the user selected


        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    private fun onLocationSelected() {
        //        TODO: When the user confirms on the selected location,
        //         send back the selected location details to the view model
        //         and navigate back to the previous fragment to save the reminder and add the geofence
    }

    private fun setupMenuOptions(menuHost: MenuHost) {
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.map_options, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean =
                when (menuItem.itemId) {
                    // TODO: Change the map type based on the user's selection.
                    R.id.normal_map -> {
                        true
                    }
                    R.id.hybrid_map -> {
                        true
                    }
                    R.id.satellite_map -> {
                        true
                    }
                    R.id.terrain_map -> {
                        true
                    }
                    else -> false
                }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    // Register the permissions callback, which handles the user's response to the
    // system permissions dialog.
    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            if (granted) {
                Log.d(TAG, "Foreground Permissions granted")
                if (backgroundPermissionArray.isNotEmpty()) {
                    requestBackgroundLocationPermission()
                }
            } else {
                // not all permissions granted
                Log.d(TAG, "Permissions not granted")
                Snackbar.make(
                    binding.mainLayout,
                    R.string.background_permission_denied,
                    Snackbar.LENGTH_INDEFINITE
                )
                    .setAction(R.string.settings) {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }.show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestForegroundLocationPermissions() {
        if (hasPermissions(permissionsArray) && hasPermissions(backgroundPermissionArray)) {
            Log.d(TAG, "Permissions already granted")
            return
        }
        // Permission has not been granted and must be requested.
        if (shouldShowRequestPermissionRationale(permissionsArray)) {
            // Provide an additional rationale to the user if the permission was not granted
            Snackbar.make(
                binding.mainLayout,
                R.string.location_required_error,
                Snackbar.LENGTH_INDEFINITE
            ).setAction(R.string.ok) {
                requestPermission.launch(permissionsArray)
            }.show()
        } else {
            // directly ask for permission
            Log.d(TAG, "Directly asking for permission")
            Snackbar.make(
                binding.mainLayout,
                R.string.location_permission_not_available,
                Snackbar.LENGTH_SHORT
            ).show()
            // Request the permission.
            requestPermission.launch(permissionsArray)
        }

    }

    private fun requestBackgroundLocationPermission() {
        if (hasPermissions(backgroundPermissionArray)) {
            Log.d(TAG, "Background permissions already granted")
            return
        }
        Snackbar.make(
            binding.mainLayout,
            R.string.background_permission_denied,
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.ok) {
            requestPermission.launch(backgroundPermissionArray)
        }.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        val sydney = LatLng(-33.852, 151.211)
        googleMap.addMarker(
            MarkerOptions()
                .position(sydney)
                .title("Marker in Sydney")
        )

    }
}
