package com.bluesolution.driver.ui.directions

import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import com.bluesolution.driver.R
import com.bluesolution.driver.databinding.ActivityMainBinding
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.NavigationViewOptions
import com.mapbox.navigation.ui.OnNavigationReadyCallback
import com.mapbox.navigation.ui.camera.Camera
import com.mapbox.navigation.ui.camera.DynamicCamera
import com.mapbox.navigation.ui.camera.NavigationCameraUpdate
import com.mapbox.navigation.ui.camera.RouteInformation
import com.mapbox.navigation.ui.listeners.NavigationListener
import com.mapbox.navigation.ui.map.NavigationMapboxMap
import retrofit2.Call
import retrofit2.Response

class MainActivity : AppCompatActivity(), OnNavigationReadyCallback, NavigationListener {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var binding: ActivityMainBinding
    private lateinit var navigationMapboxMap: NavigationMapboxMap
    private lateinit var mapboxNavigation: MapboxNavigation
    private lateinit var route: DirectionsRoute
    private lateinit var origin: Point
    private lateinit var waypoint: Point
    private lateinit var destination: Point
    private lateinit var client: MapboxDirections

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(applicationContext, getString(R.string.mapbox_access_token))
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        origin = Point.fromLngLat(120.53376974852144, 15.175695148344897)
        waypoint = Point.fromLngLat(120.53013067050756, 15.176981074967449)
        destination = Point.fromLngLat(120.52077627959572, 15.177812805063475)
        binding.navigationView.onCreate(savedInstanceState)
        binding.navigationView.initialize(this)
        binding.startTrip.setOnClickListener {
            val optionsBuilder = NavigationViewOptions.builder(this@MainActivity)
            optionsBuilder.navigationListener(this@MainActivity)
            optionsBuilder.directionsRoute(route)
            optionsBuilder.locationObserver(locationObserver)
            optionsBuilder.arrivalObserver(arrivalObserver)
            optionsBuilder.camera(MyCamera(navigationMapboxMap.retrieveMap()))
            optionsBuilder.shouldSimulateRoute(true)
            binding.navigationView.startNavigation(optionsBuilder.build())

        }
    }
    private val locationObserver = object : LocationObserver{
        override fun onEnhancedLocationChanged(enhancedLocation: Location, keyPoints: List<Location>) {
            Log.d(TAG, "enhancedlocationchange latitude: ${enhancedLocation.latitude}, enhancedlocationchange longitude: ${enhancedLocation.longitude}")
        }

        override fun onRawLocationChanged(rawLocation: Location) {
            Log.d(TAG, "rawlocationchange latitude: ${rawLocation.latitude}, rawlocationchange longitude: ${rawLocation.longitude}")
        }
    }

    val arrivalObserver = object : ArrivalObserver {
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            Log.d(TAG,"final destination arrival: $routeProgress")
            binding.navigationView.stopNavigation()
        }
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) {
            Log.d(TAG,"nextrouteleg start: $routeLegProgress")
        }
    }
    fun getRoute(origin:Point, waypoint: Point, destination: Point){
        client = MapboxDirections.builder()
            .origin(origin)
            .addWaypoint(waypoint)
            .destination(destination)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .profile(DirectionsCriteria.PROFILE_DRIVING)
            .steps(true)
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
                val response = response
                val currentRoute = response.body()!!.routes()[0]
                route = currentRoute
                Log.d(TAG, "route geometry: ${route.geometry()?.length}")
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
        getRoute(origin, waypoint, destination)
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