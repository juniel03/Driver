package com.bluesolution.driver.ui.directions

import android.location.Geocoder
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.bluesolution.driver.R
import com.bluesolution.driver.data.models.Coordinates
import com.bluesolution.driver.databinding.ActivityMainBinding
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.camera.Camera
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCameraUpdate
import com.mapbox.navigation.ui.camera.RouteInformation
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import dagger.hilt.android.AndroidEntryPoint
import retrofit2.Call
import retrofit2.Response

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), OnNavigationReadyCallback, NavigationListener {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var route: DirectionsRoute
    private lateinit var origin: Point
    private lateinit var store: Point
    private lateinit var destination: Point
    private lateinit var client: MapboxDirections

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        //replace origin with driver's current location
        origin = Point.fromLngLat(120.53376974852144, 15.175695148344897)
        binding.navigationView.onCreate(savedInstanceState)
        binding.navigationView.initialize(this)
        binding.acceptOrder.setOnClickListener {
            viewModel.accept(true)
            viewModel.getUserCoord(object : MainActivityViewModel.onGetDataListener{
                override fun onSuccess(coordinates: Coordinates) {
                    super.onSuccess(coordinates)
                    Log.d("tag", "user coordinates $coordinates")
                    destination = Point.fromLngLat(coordinates.longitude, coordinates.latitude)
                    binding.start.isEnabled = true
                    binding.acceptOrder.isEnabled = false
                }
            })
            viewModel.getStoreCoord(object: MainActivityViewModel.onGetDataListener{
                override fun onSuccess(coordinates: Coordinates) {
                    super.onSuccess(coordinates)
                    Log.d("tag", "store coordinates $coordinates")
                    store = Point.fromLngLat(coordinates.longitude, coordinates.latitude)
                    binding.start.isEnabled = true
                }
            })
        }
        binding.start.setOnClickListener {
            Log.d(TAG, "origin: $origin")
            Log.d(TAG, "destination $destination")
            Log.d(TAG, "store: $store")
            binding.orders.text = "Pick up Order at the store"
            binding.start.isEnabled = false
        getRoutetoStore(origin, store)
        }
        binding.deliver.setOnClickListener {
            getRoutetoDestination(store,destination)
            viewModel.pickedUp(true)
            binding.orders.text = "Deliver the user's order"
            binding.deliver.isEnabled = false
        }

        viewModel.fetchData().observe(this, Observer {
            Log.d("tag", "it: ${it.accepted}")
           if (!it.accepted){
               binding.acceptOrder.isEnabled = true
               binding.orders.text = "You have an order"
           }
        })
    }
    private val locationObserver = object : LocationObserver{
        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            Log.d(TAG, "enhancedlocationchange latitude: ${enhancedLocation.latitude}, enhancedlocationchange longitude: ${enhancedLocation.longitude}")
        }
        override fun onRawLocationChanged(rawLocation: Location) {
        }
    }

    val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            Log.d(TAG,"final destination arrival: $routeProgress")
            binding.deliver.isEnabled = true
        }
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            Log.d(TAG,"nextrouteleg start: $routeLegProgress")
            binding.navigationView.setWayNameVisibility(true)
        }
    }

    private val locationObserverDestination = object : LocationObserver{
        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            Log.d(TAG, "enhancedlocationchange latitude: ${enhancedLocation.latitude}, enhancedlocationchange longitude: ${enhancedLocation.longitude}")
            viewModel.uploadCoordinates(Coordinates(enhancedLocation.latitude, enhancedLocation.longitude))

        }
        override fun onRawLocationChanged(rawLocation: Location) {
        }
    }

    val arrivalObserverDestination = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            viewModel.arrived(true)
            Log.d(TAG,"final destination arrival: $routeProgress")
            binding.navigationView.stopNavigation()
            binding.deliver.isEnabled = false
            binding.orders.text = "You have arrived at the user's address"
            Toast.makeText(this@MainActivity, "ARRIVED", Toast.LENGTH_LONG).show()
        }
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            Log.d(TAG,"nextrouteleg start: $routeLegProgress")
        }
    }

    fun getRoutetoStore(origin:Point, store: Point){
        client = MapboxDirections.builder()
            .origin(origin)
            .destination(store)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .geometries(RouteUrl.GEOMETRY_POLYLINE6)
            .steps(true)
            .voiceUnits(DirectionsCriteria.METRIC)
            .voiceInstructions(true)
            .accessToken(getString(R.string.mapbox_access_token))
            .build()
        client.enqueueCall(object : retrofit2.Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null) {
                    Log.d(TAG, "response null, make sure you set the right user and access token.")
                    return
                } else if (response.body()!!.routes().size < 1) {
                    Log.d(TAG, "No routes found")
                    return
                }
                // Get the directions route
                val currentRoute = response.body()!!.routes()[0]
                //start Navigation
                val optionsBuilder = NavigationViewOptions.builder(this@MainActivity)
                optionsBuilder.navigationListener(this@MainActivity)
                optionsBuilder.directionsRoute(currentRoute)
                optionsBuilder.locationObserver(locationObserver)
                optionsBuilder.arrivalObserver(arrivalObserver)
                optionsBuilder.camera(MyCamera(navigationMapboxMap.retrieveMap()))
                optionsBuilder.shouldSimulateRoute(true)
                binding.navigationView.startNavigation(optionsBuilder.build())
            }
            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Log.e(TAG,"Error: " + throwable.message)
            }
        })
    }
    fun getRoutetoDestination(store:Point, destination: Point){
        client = MapboxDirections.builder()
                .origin(store)
                .destination(destination)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .geometries(RouteUrl.GEOMETRY_POLYLINE6)
                .steps(true)
                .voiceUnits(DirectionsCriteria.METRIC)
                .voiceInstructions(true)
                .accessToken(getString(R.string.mapbox_access_token))
                .build()
        client.enqueueCall(object : retrofit2.Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null) {
                    Log.d(TAG, "response null, make sure you set the right user and access token.")
                    return
                } else if (response.body()!!.routes().size < 1) {
                    Log.d(TAG, "No routes found")
                    return
                }
                // Get the directions route
                val currentRoute = response.body()!!.routes()[0]
                route = currentRoute
                currentRoute.geometry()?.let {
                    viewModel.sendRoute(it)
                }
                Log.d(TAG, "whole route geometry: ${currentRoute.geometry()}")
                //start Navigation
                val optionsBuilder = NavigationViewOptions.builder(this@MainActivity)
                optionsBuilder.navigationListener(this@MainActivity)
                optionsBuilder.directionsRoute(currentRoute)
                optionsBuilder.locationObserver(locationObserverDestination)
                optionsBuilder.arrivalObserver(arrivalObserverDestination)
                optionsBuilder.camera(MyCamera(navigationMapboxMap.retrieveMap()))
                optionsBuilder.shouldSimulateRoute(true)
                binding.navigationView.startNavigation(optionsBuilder.build())
            }
            override fun onFailure(call: Call<DirectionsResponse>, throwable: Throwable) {
                Log.e(TAG,"Error: " + throwable.message)
            }
        })
    }

    override fun onNavigationReady(isRunning: Boolean) {
        if (binding.navigationView.retrieveMapboxNavigation() != null){
            mapboxNavigation = binding.navigationView.retrieveMapboxNavigation()!!
        }else{
            val n : NavigationOptions = MapboxNavigation.defaultNavigationOptionsBuilder(this, getString(R.string.mapbox_access_token)).build()
            mapboxNavigation = MapboxNavigation(n)
        }
        navigationMapboxMap = binding.navigationView.retrieveNavigationMapboxMap()!!
    }
    class MyCamera(mapboxMap: MapboxMap) : DynamicCamera(mapboxMap){
        override fun zoom(routeInformation: RouteInformation): Double {
            return Math.max(super.zoom(routeInformation), MIN_ZOOM)
        }
        companion object {
            val MIN_ZOOM = 15.0
        }
    }

    override fun onNavigationFinished() {
        finish()
    }

    override fun onNavigationRunning() {
    }

    override fun onCancelNavigation() {
        binding.navigationView.stopNavigation()
        finish()
    }


    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.navigationView.onLowMemory()
    }

    override fun onStart() {
        super.onStart()
        binding.navigationView.onStart()
    }

    override fun onResume() {
        super.onResume()
        binding.navigationView.onResume()
    }

    override fun onStop() {
        super.onStop()
        binding.navigationView.onStop()
        mapboxNavigation.unregisterLocationObserver(locationObserver)
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
    }

    override fun onPause() {
        super.onPause()
        binding.navigationView.onPause()
    }

    override fun onDestroy() {
        binding.navigationView.onDestroy()
        viewModel.delete()
        super.onDestroy()
    }

    override fun onBackPressed() {
// If the navigation view didn't need to do anything, call super
        if (!binding.navigationView.onBackPressed()) {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding.navigationView.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.navigationView.onRestoreInstanceState(savedInstanceState)
    }
}