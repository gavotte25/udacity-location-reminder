package com.udacity.project4.locationreminders.savereminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.base.NavigationCommand
import com.udacity.project4.databinding.FragmentSaveReminderBinding
import com.udacity.project4.locationreminders.geofence.GeofenceBroadcastReceiver
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.savereminder.selectreminderlocation.SelectLocationFragment
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject

class SaveReminderFragment : BaseFragment() {
    //Get the view model this time as a single to be shared with the another fragment
    override val _viewModel: SaveReminderViewModel by inject()
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
        binding.selectLocation.setOnClickListener {
            //            Navigate to another fragment to get the user location
            _viewModel.navigationCommand.value =
                NavigationCommand.To(SaveReminderFragmentDirections.actionSaveReminderFragmentToSelectLocationFragment())
        }

        binding.saveReminder.setOnClickListener {
            val reminder = ReminderDataItem(
                    _viewModel.reminderTitle.value,
                    _viewModel.reminderDescription.value,
                    _viewModel.reminderSelectedLocationStr.value,
                    _viewModel.latitude.value,
                    _viewModel.longitude.value
            )
            if(_viewModel.validateEnteredData(reminder)) {
                checkPermissionAndSave(reminder)
            }
        }
    }

    /**
     * If required permissions are granted, add Geofence, if success, add to local database
     */
    private fun checkPermissionAndSave(reminder: ReminderDataItem) {
        val geofence = getGeofence(reminder)
        val geofenceRequest = getGeoFencingRequest(geofence)
        if ((ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) ||
                (runningQorLater() && ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                    Log.i("PERRY", "checkPermissionAndAddGeoFence true")
                    if(runningQorLater()) {
                        Log.i("PERRY", "requestPermissionFromQAndAbove called")
                        requestPermissionFromQAndAbove()
                    } else {
                        Log.i("PERRY", "requestPermissionBelowQ called")
                        requestPermissionBelowQ()
                    }
        } else {
            Log.i("PERRY", "checkPermissionAndAddGeoFence false")
            geofencingClient.addGeofences(geofenceRequest, geofencePendingIntent).run {
                addOnSuccessListener {
                    _viewModel.saveReminder(reminder)
                }
                addOnFailureListener{
                    _viewModel.showErrorMessage.value = "Failed setting up the job"
                }
            }
        }
    }

    private fun runningQorLater(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    }

    private fun requestPermissionBelowQ() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun requestPermissionFromQAndAbove() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        Log.i("PERRY", "onRequestPermissionsResult called")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if ((requestCode != LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE && runningQorLater())||
                requestCode != LOCATION_PERMISSION_REQUEST_CODE && !runningQorLater()) {
            Log.i("PERRY", "request not called")
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
                .setExpirationDuration(GEOFENCE_EXPIRATION_IN_MILLISECONDS)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
    }

    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1088
        const val LOCATION_BACKGROUND_PERMISSION_REQUEST_CODE= 1089
        const val GEOFENCE_RADIUS_IN_METERS = 100f
        const val GEOFENCE_EXPIRATION_IN_MILLISECONDS = 120000L
    }
}
