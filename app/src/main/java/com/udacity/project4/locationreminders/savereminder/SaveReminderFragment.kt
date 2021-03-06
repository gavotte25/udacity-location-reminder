package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.androidx.viewmodel.ext.android.sharedViewModel

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by sharedViewModel()
    private lateinit var binding: FragmentSaveReminderBinding

    private lateinit var geofencingClient: GeofencingClient
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_save_reminder, container, false)

        setDisplayHomeAsUpEnabled(true)

        binding.viewModel = _viewModel

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = this
        binding.llLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            onSaveClickListener()
        }
    }

    /**
     * Check valid data, permission and location settings then save and add geofence
     * @param resolve to decide whether Location setting request should be called
     */
    private fun onSaveClickListener(resolve: Boolean = true) {
        val reminder = ReminderDataItem(
                _viewModel.reminderTitle.value,
                _viewModel.reminderDescription.value,
                _viewModel.reminderSelectedLocationStr.value,
                _viewModel.latitude.value,
                _viewModel.longitude.value
        )
        if(_viewModel.validateEnteredData(reminder)) {
            if (arePermissionsGranted()) {
                checkLocationSettingsAndAddGeofence(reminder, resolve)
            }
        }
    }

    /**
     * If required permissions are granted, add Geofence, if success, add to local database
     */
    private fun arePermissionsGranted(): Boolean {
        var permissionsGranted = false
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (runningQorLater() && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                    if(runningQorLater()) {
                        requestPermissionFromQAndAbove()
                    } else {
                        requestPermissionBelowQ()
                    }
        } else {
            permissionsGranted = true
        }
        return permissionsGranted
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence(reminder: ReminderDataItem) {
        val geofence = getGeofence(reminder)
        val geofenceRequest = getGeoFencingRequest(geofence)
        geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
            addOnSuccessListener {
                _viewModel.saveReminder(reminder)
            }
            addOnFailureListener{
                _viewModel.showErrorMessage.value = "Failed setting up the job"
                Log.e("SaveReminderFragment", "Failed setting up: ", it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TURN_DEVICE_LOCATION_ON) {
            onSaveClickListener(false)
        }
    }

    private fun checkLocationSettingsAndAddGeofence(reminder: ReminderDataItem, resolve: Boolean) {
        val locationRequest = LocationRequest.create().apply { priority = LocationRequest.PRIORITY_LOW_POWER }
        val requestBuilder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingClient = LocationServices.getSettingsClient(requireActivity())
        val locationSettingsResponseTask = settingClient.checkLocationSettings(requestBuilder.build())
        locationSettingsResponseTask.apply {
            addOnCompleteListener {
                if(it.isSuccessful) {
                    addGeofence(reminder)
                }
            }
            addOnFailureListener { exception ->
                if(exception is ResolvableApiException && resolve) {
                    try {
                        startIntentSenderForResult(exception.resolution.intentSender, REQUEST_TURN_DEVICE_LOCATION_ON, null, 0,0, 0, null)
                    } catch (sendEx: IntentSender.SendIntentException) {
                        Log.d("SaveReminderFragment", "Error getting location settings resolution: " + sendEx.message)
                    }
                } else {
                    _viewModel.showErrorMessage.value = "Please enable location to save reminder!"
                }
            }
        }
    }

    private fun runningQorLater(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun requestPermissionBelowQ() {
        requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestPermissionFromQAndAbove() {
        requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode == LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE && runningQorLater())||
                requestCode == LOCATION_PERMISSION_REQUEST_CODE && !runningQorLater()) {
            onSaveClickListener()
            return
        }
        if (grantResults.contains(PackageManager.PERMISSION_DENIED)) {
            _viewModel.showErrorMessage.value = "Missing permission to set reminder, please retry!"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //make sure to clear the view model after destroy, as it's a single view model.
        _viewModel.onClear()
    }

    private fun getGeoFencingRequest(geofence: Geofence): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofence(geofence)
        }.build()
    }

    private fun getGeofence(reminder: ReminderDataItem): Geofence {
        return Geofence.Builder()
                .setRequestId(reminder.id)
                .setCircularRegion(reminder.latitude!!, reminder.longitude!!, GEOFENCE_RADIUS_IN_METERS)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1088
        const val LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE= 1089
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        const val REQUEST_TURN_DEVICE_LOCATION_ON = 1593
    }
}
