package com.bluesolution.driver.ui.user

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Color.parseColor
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.bluesolution.driver.R
import com.bluesolution.driver.databinding.ActivityUserBinding
import com.mapbox.geojson.LineString
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions

import com.utsman.smartmarker.mapbox.Marker
import com.utsman.smartmarker.mapbox.MarkerOptions
import com.utsman.smartmarker.mapbox.addMarker
import dagger.hilt.android.AndroidEntryPoint

import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.bluesolution.driver.data.models.Coordinates
import com.bluesolution.driver.data.models.Order
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback


@AndroidEntryPoint
class User : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var binding: ActivityUserBinding
    private lateinit var symbolManager: SymbolManager
    private var marker: Marker? = null
    private val viewModel: UserViewModel by viewModels()
    private lateinit var reference: DatabaseReference
    private var mapboxMap: MapboxMap? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.mapbox_access_token))
        binding = ActivityUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        reference = FirebaseDatabase.getInstance().reference
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync (this)
        binding.order.setOnClickListener {
            viewModel.order(reference, Order(false,
                    false,
                    "n/a",
                    false,
                    Coordinates(15.177812805063475,120.52077627959572),
                    Coordinates(15.177892711093946,120.5297054235839),
                    Coordinates(15.177892711093946,120.5297054235839)))
            binding.status.text = "order status: ordered"

            viewModel.getPickedUpStatus().observe(this, Observer {
                Log.d("tag", "pickedUp on User $it")
                if (it){
                    binding.status.text = "Your Rider has picked up your order and is on the way!"
                    viewModel.getDriverCoord().observe(this, Observer {
                        Log.d("tag", "Coordinates: ${it.latitude} , ${it.longitude}")
                        marker!!.moveMarkerSmoothly(LatLng(it.latitude, it.longitude), true)
                        var cameraPosition = CameraPosition.Builder()
                                .target(LatLng(it.latitude, it.longitude))
                                .zoom(16.0)
                                .build()
                        mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 5000)
                    })
                }
            })
            viewModel.getPath(object: UserViewModel.onGetDataListener{
                override fun onSuccess(path: String) {
                    super.onSuccess(path)
                    Log.d("tag", "path $path")
                    if (path != "n/a"){
                        val routeLineString = LineString.fromPolyline(path,
                                6)
                        val coordinates = routeLineString.coordinates()
                        val markerOption = MarkerOptions.Builder() // from 'com.utsman.smartmarker.mapbox.MarkerOptions'
                                .setId("marker-id", true)
                                .setIcon(R.drawable.ic_car, true)
                                .setPosition(latLng = LatLng(coordinates[0].latitude(), coordinates[0].longitude()))
                                .build(this@User)
                        marker = mapboxMap?.addMarker(markerOption)

                        val points1: ArrayList<LatLng> = ArrayList()
                        for (coordinates in coordinates) {
                            points1.add(LatLng(coordinates.latitude(), coordinates.longitude()))
                        }
                        val polylineOptions = PolylineOptions()
                                .addAll(points1)
                                .color(Color.YELLOW)
                                .width(3f)
                        mapboxMap?.addPolyline(polylineOptions)
                    }
                }
            })

            viewModel.getAcceptStatus().observe(this, Observer {
                Log.d("tag", "accept on User $it")
                if(it){
                    binding.status.text = "A rider has accepted your order!!"
                }
            })
            viewModel.getDeliveredStatus().observe(this, Observer {
                if (it){
                    binding.status.text = "Your Rider is at your doorstep!"
                }
            })
        }

    }
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.mapView.onDestroy()
    }
    companion object {
        private const val TAG = "UserActivity"
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.MAPBOX_STREETS){
            mapboxMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(15.177812805063475,120.52077627959572))
                    .zoom(15.0)
                    .build()
//            symbolManager = SymbolManager(binding.mapView, mapboxMap, it)
//            symbolManager.iconAllowOverlap = true
//            symbolManager.textAllowOverlap = true
//            it.addImage("myImage", BitmapFactory.decodeResource(resources,R.drawable.mapbox_marker_icon_default))
//            symbolManager.create(SymbolOptions()
//                    .withLatLng(LatLng(15.177812805063475,120.52077627959572))
//                    .withIconImage("myImage"))
            val markerOption = MarkerOptions.Builder() // from 'com.utsman.smartmarker.mapbox.MarkerOptions'
                    .setId("marker-id", true)
                    .setIcon(R.drawable.mapbox_marker_icon_default, true)
                    .setPosition(latLng = LatLng(15.177812805063475,120.52077627959572))
                    .build(this@User)
            marker = mapboxMap.addMarker(markerOption)
        }
    }

}