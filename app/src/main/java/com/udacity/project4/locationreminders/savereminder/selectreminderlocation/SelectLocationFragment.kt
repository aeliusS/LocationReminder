package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
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
import androidx.navigation.fragment.findNavController
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
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.sharedViewModel
import java.util.*

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    companion object {
        private val TAG = SelectLocationFragment::class.java.simpleName
        // Keys for storing activity state.
        private const val KEY_CAMERA_POSITION = "CAMERA_POSITION"
        private const val KEY_LOCATION = "LOCATION"
        private const val DEFAULT_ZOOM = 15f

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

    private var cameraPosition: CameraPosition? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private val defaultLocation = LatLng(-33.8523341, 151.2106085)
    private var lastKnownLocation: Location? = null

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel by inject<SaveReminderViewModel>()
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

        Log.d(TAG, "Title is ${_viewModel.reminderTitle.value}")

        // retrieve location and camera position from saved instance state
        if (savedInstanceState != null) {
            lastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            cameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION)
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // Get the SupportMapFragment and request notification when the map is ready to be used.
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        _viewModel.locationPermissionGranted.observe(viewLifecycleOwner) { updateLocationUI() }

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
        // Log.d(TAG, "onLocationSelected called")
        findNavController().popBackStack(R.id.saveReminderFragment, false)
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
    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val granted = permissions.entries.all {
                it.value
            }
            // check if background location was requested in this map
            val background = permissions.entries.any {
                it.key == "android.permission.ACCESS_BACKGROUND_LOCATION"
            }
            if (granted) {
                Log.d(TAG, "Permissions granted")
                _viewModel.updateLocationGrantedTo(true)
                // if background location wasn't already granted
                if (!background && backgroundPermissionArray.isNotEmpty()) {
                    requestBackgroundLocationPermission()
                }
            } else {
                // not all permissions granted
                Log.d(TAG, "Permissions not granted")

                // if at least foreground location permission was granted, update the live data variable
                _viewModel.updateLocationGrantedTo(false)

                Snackbar.make(
                    binding.mainLayout,
                    if (background) R.string.background_permission_denied else R.string.location_required_error,
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
            _viewModel.updateLocationGrantedTo(true)
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
                requestPermissions.launch(permissionsArray)
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
            requestPermissions.launch(permissionsArray)
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
            requestPermissions.launch(backgroundPermissionArray)
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

        // setup map interactions
        // setMapClick() // we only care about point of interest (poi) so leave this function unused
        setPoiClick()

        // load style
        setMapStyle()

        // turn on the my location layer and get current location of the device
        updateLocationUI()
        getDeviceLocation()
    }

    private fun updateLocationUI() {
        if (map == null) return
        try {
            if (_viewModel.locationPermissionGranted.value == true) {
                Log.d(TAG, "Location permission granted")
                map?.apply {
                    isMyLocationEnabled = true
                    uiSettings.isMyLocationButtonEnabled = true
                }
            } else {
                map?.apply {
                    isMyLocationEnabled = false
                    uiSettings.isMyLocationButtonEnabled = false
                }
                lastKnownLocation = null
                // request runtime permissions if necessary; API level 23 (M) and higher
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestForegroundLocationPermissions()
                }
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun setMapClick() {
        map?.setOnMapClickListener { latLng ->
            _viewModel.selectedMarker.value?.remove()
            val snippet = getStreetAddress(latLng)
            _viewModel.selectedMarker.value = map?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(getString(R.string.dropped_pin))
                    .snippet(snippet)
            )
            _viewModel.selectedMarker.value?.showInfoWindow()
            _viewModel.updateChosenLocation(latLng, snippet)
            showChooseLocationDialog(snippet)
        }
    }

    private fun setPoiClick() {
        map?.setOnPoiClickListener { poi ->
            _viewModel.selectedMarker.value?.remove()
            val snippet = getStreetAddress(poi.latLng)
            val title = poi.name.replace("\n"," ")
            _viewModel.selectedMarker.value = map?.addMarker(
                MarkerOptions()
                    .position(poi.latLng)
                    .title(title)
                    .snippet(snippet)
            )
            _viewModel.selectedMarker.value?.showInfoWindow()
            _viewModel.updateChosenLocation(poi.latLng, title)
            showChooseLocationDialog(title)
        }
    }

    /**
     * Show a dialog that asks the user to confirm the location selected
     * */
    private fun showChooseLocationDialog(snippet: String) {
        Snackbar.make(
            binding.mainLayout,
            getString(R.string.location_selected_confirmation, snippet),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(R.string.ok) {
            onLocationSelected()
        }.show()
    }

    private fun getStreetAddress(latLng: LatLng) : String {
        val address = geocoder?.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val street = if (address?.isNotEmpty() == true) {
            address[0].getAddressLine(0)
        } else {
            String.format(
                Locale.getDefault(),
                getString(R.string.lat_long_snippet),
                latLng.latitude,
                latLng.longitude
            )
        }
        return String.format(Locale.getDefault(),street)
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

    private fun getDeviceLocation() {
        try {
            if (_viewModel.locationPermissionGranted.value == true) {
                val locationResult = fusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        // Set the map's camera position to the current location of the device.
                        lastKnownLocation = task.result
                        if (lastKnownLocation != null) {
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(lastKnownLocation!!.latitude,
                                lastKnownLocation!!.longitude), DEFAULT_ZOOM
                            ))
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s", task.exception)
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, DEFAULT_ZOOM))
                            map?.uiSettings?.isMyLocationButtonEnabled = false
                        }
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG + "Exception: %s", e.message, e)
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
