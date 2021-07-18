package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import com.udacity.project4.utils.zoomToLocation
import org.koin.android.ext.android.inject

class SelectLocationFragment : BaseFragment(), OnMapReadyCallback {

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private var selectedPosition: LatLng? = null
    private var selectedPostionDescrition: String = ""
    private lateinit var binding: FragmentSelectLocationBinding
    private lateinit var map: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this
        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        return binding.root
    }

    private fun onLocationSelected() {
        selectedPosition?.apply {
            _viewModel.latitude.value = this.latitude
            _viewModel.longitude.value = this.longitude
        }
        _viewModel.reminderSelectedLocationStr.value = selectedPostionDescrition
        _viewModel.navigationCommand.value = NavigationCommand.Back
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.normal_map -> {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            map.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onMapReady(p0: GoogleMap?) {
        if (p0 == null) return
        map = p0
        map.setOnMapLongClickListener(this::addMarker)
        map.setOnMapClickListener{hideButtonAndReset()}
        map.setOnMarkerClickListener(this::onMarkerClick)
        map.setOnPoiClickListener(this::addPoiMarker)
        binding.btnSave.setOnClickListener{onLocationSelected()}
        setMapStyle()
        enableMyLocation()
    }

    private fun getLatLngString(latLng: LatLng): String {
        return String.format("Lat: 1%.5f. Long: 2%.5f", latLng.latitude, latLng.longitude)
    }

    private fun addPoiMarker(poi: PointOfInterest) {
        map.addMarker(MarkerOptions().position(poi.latLng).snippet(getLatLngString(poi.latLng)).title(poi.name))
                .setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        hideButtonAndReset()
    }

    /**
     * Hide the button and reset selectedPosition value when click somewhere else besides markers
     */
    private fun hideButtonAndReset() {
        if (selectedPosition != null) {
            selectedPosition = null
            selectedPostionDescrition = ""
            binding.btnSave.visibility = View.GONE
        }
    }

    private fun addMarker(latLng: LatLng) {
        map.addMarker(MarkerOptions().position(latLng).snippet(getLatLngString(latLng)).title("Unknown"))
        hideButtonAndReset()
    }

    /**
     * Delete the marker if it is clicked while its InfoWindow has been already showing. Otherwise
     * InfoWindow is popup (as default behaviour) and popup save button
     */
    private fun onMarkerClick(marker: Marker): Boolean {
        var result = false
        if (selectedPosition == marker.position) {
            marker.remove()
            hideButtonAndReset()
            result = true
        }
        else {
            selectedPosition = marker.position
            selectedPostionDescrition = marker.title
            binding.btnSave.visibility = View.VISIBLE
        }
        return result
    }

    /**
     * enable and zoom into my location
     */
    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION),
                    LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            map.isMyLocationEnabled = true
            FusedLocationProviderClient(requireContext()).lastLocation.addOnSuccessListener {
                map.zoomToLocation(it)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) return
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            _viewModel.showErrorMessage.value = "Cannot retrieve current location. Please enable location permission!"
        } else {
            enableMyLocation()
        }
    }

    private fun setMapStyle() {
        try {
            val success = map.setMapStyle(MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style))
            if (!success) {
                Log.e(TAG, "Style parsing failed")
            }
        }
        catch (e: Resources.NotFoundException) {
            Log.e(TAG, "Can't find map style. Error: ", e)
        }
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 2502
        val TAG: String = SelectLocationFragment::class.java.simpleName
    }


}
