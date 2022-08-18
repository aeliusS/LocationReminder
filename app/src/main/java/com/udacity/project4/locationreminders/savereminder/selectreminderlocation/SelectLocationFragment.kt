package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.location.Geocoder
import android.location.Location
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
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
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "CAMERA_POSITION"
        private const val KEY_LOCATION = "LOCATION"

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

    private var map: GoogleMap? = null
    private var geocoder: Geocoder? = null
    private var selectedMarker: Marker? = null

    private var cameraPosition: CameraPosition? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    // TODO: use these variables
    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var lastKnownLocation: Location? = null
    private var locationPermissionGranted = false

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

        // retrieve location and camera position from saved instance state
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        // TODO: call this function after the user confirms on the selected location
        onLocationSelected()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        map?.let { map ->
            outState.putParcelable(KEY_CAMERA_POSITION, map.cameraPosition)
            outState.putParcelable(KEY_LOCATION, lastKnownLocation)
        }
        super.onSaveInstanceState(outState)
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
                    R.id.normal_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_NORMAL
                        true
                    }
                    R.id.hybrid_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_HYBRID
                        true
                    }
                    R.id.satellite_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_SATELLITE
                        true
                    }
                    R.id.terrain_map -> {
                        map?.mapType = GoogleMap.MAP_TYPE_TERRAIN
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
        if (hasPermissions(permissionsArray.plus(backgroundPermissionArray))) {
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
        map = googleMap
        Log.d(TAG, "Map ready")

        geocoder = Geocoder(requireContext(), Locale.getDefault())

        // request runtime permissions if necessary; API level 23 (M) and higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestForegroundLocationPermissions()
        }

        setMapClick()
        setMapStyle()
        setPoiClick()
        enableMyLocation()
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (hasPermissions(permissionsArray)) {
            map?.isMyLocationEnabled = true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestForegroundLocationPermissions()
        }
    }

    // TODO: finish this function
    private fun updateLocationUI() {
        if (map == null) return
        try {

        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun setMapClick() {
        map?.setOnMapClickListener { latLng ->
            selectedMarker?.remove()
            val address = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
            val street = if (address?.isNotEmpty() == true) address[0].getAddressLine(0) else getString(R.string.lat_long_snippet)
            val snippet = String.format(Locale.getDefault(),street)

            selectedMarker = map?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )
            selectedMarker?.showInfoWindow()
            //map.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        }
    }

    private fun setMapStyle() {
        try {
            val success = map?.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireContext(),
                    R.raw.map_style
                )
            )
            if (success == false) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find style. Error: ", e)
        }
    }

    private fun setPoiClick() {
        map?.setOnPoiClickListener { poi ->
            selectedMarker?.remove()
            selectedMarker = map?.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(poi.name)
            )
            selectedMarker?.showInfoWindow()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onResume() {
        super.onResume()
        if (!hasPermissions(permissionsArray.plus(backgroundPermissionArray))) {
            requestForegroundLocationPermissions()
        }
    }
}
