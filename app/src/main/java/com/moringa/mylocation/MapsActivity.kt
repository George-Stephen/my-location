package com.moringa.mylocation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.IOException
import java.lang.ClassCastException
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, PermissionListener{

    companion object{
        const val REQUEST_CHECK_SETTINGS = 43
    }

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment!!.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap?: return
        if(isPermissionGranted()){
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
            getCurrentLocation()
        }else{
            grantPermission()
        }

    }
    private fun isPermissionGranted() : Boolean {
        return ActivityCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun grantPermission(){
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(this)
            .check()
    }

    override fun onPermissionGranted(response: PermissionGrantedResponse?) {
        getCurrentLocation()
    }

    override fun onPermissionRationaleShouldBeShown(
        permission: PermissionRequest?,
        token: PermissionToken?
    ) {
        token!!.continuePermissionRequest()
    }

    override fun onPermissionDenied(response: PermissionDeniedResponse?) {
        Toast.makeText(this,"Permission is required to show location",Toast.LENGTH_LONG).show()
        finish()
    }
    private fun getCurrentLocation(){
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = (10*1000).toLong()
        locationRequest.fastestInterval = 2000

        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        val locationSettingsRequest = builder.build()

        val result = LocationServices.getSettingsClient(this).checkLocationSettings(locationSettingsRequest)
        result.addOnCompleteListener { task ->
            try{
                val response = task.getResult(ApiException :: class.java)
                if(response!!.locationSettingsStates.isLocationPresent){
                    getFinalLocation()
                }
            } catch(exception : ApiException){
                when(exception.statusCode){
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try{
                        val resolvable = exception as ResolvableApiException
                        resolvable.startResolutionForResult(this, REQUEST_CHECK_SETTINGS)
                    } catch(e: IntentSender.SendIntentException){
                    }catch(e : ClassCastException){
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE ->{}
                }
            }
        }
    }
        private fun getFinalLocation(){
            fusedLocationProviderClient.lastLocation.addOnCompleteListener(this){task ->
                if(task.isSuccessful && task.result != null){
                    val mLastLocation = task.result
                    val address = LatLng(mLastLocation!!.latitude,mLastLocation.longitude)

                    val gcd = Geocoder(this, Locale.getDefault())
                    val addresses : List<Address>

                        addresses =  gcd.getFromLocation(mLastLocation!!.latitude,mLastLocation.longitude,1)

                    val icon = BitmapDescriptorFactory.defaultMarker()
                    
                    val locationAddress = addresses[0].getAddressLine(0)



                    mMap.addMarker(
                        MarkerOptions().position(address)
                            .title("my Current location")
                            .snippet(locationAddress)
                            .icon(icon))

                    val cameraPosition = CameraPosition.Builder()
                        .target(LatLng(mLastLocation.latitude,mLastLocation.longitude))
                        .zoom(18F)
                        .build()

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
                }else {
                    Toast.makeText(this,"No current location found",Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        when(requestCode) {
            REQUEST_CHECK_SETTINGS ->{
                if(resultCode == Activity.RESULT_OK){
                        getCurrentLocation()
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)


    }

}