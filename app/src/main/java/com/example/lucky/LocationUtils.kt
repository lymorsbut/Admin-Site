package com.example.lucky

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Utility class for location-related operations
 */
object LocationUtils {
    private const val LOCATION_PREFS = "location_preferences"
    private const val KEY_LAST_LAT = "last_latitude"
    private const val KEY_LAST_LNG = "last_longitude"
    private const val KEY_LAST_TIMESTAMP = "last_timestamp"
    private const val KEY_FIRST_UPLOAD = "first_upload_done"
    
    /**
     * Calculate distance between two points in meters using the Haversine formula
     */
    fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }
    
    /**
     * Save location to SharedPreferences
     */
    fun saveLocationToPrefs(context: Context, latitude: Double, longitude: Double) {
        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_LAST_LAT, latitude.toFloat())
            putFloat(KEY_LAST_LNG, longitude.toFloat())
            putLong(KEY_LAST_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
        Log.e("LocationUtils", "Saved location to prefs: lat=$latitude, lng=$longitude")
    }
    
    /**
     * Get last saved location from SharedPreferences
     * @return Pair of (latitude, longitude) or null if no location saved
     */
    fun getLastLocationFromPrefs(context: Context): Pair<Double, Double>? {
        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_LAST_LAT, Float.MIN_VALUE)
        val lng = prefs.getFloat(KEY_LAST_LNG, Float.MIN_VALUE)
        
        return if (lat != Float.MIN_VALUE && lng != Float.MIN_VALUE) {
            Log.e("LocationUtils", "Retrieved last location from prefs: lat=$lat, lng=$lng")
            Pair(lat.toDouble(), lng.toDouble())
        } else {
            Log.e("LocationUtils", "No previous location found in prefs")
            null
        }
    }
    
    /**
     * Check if this is the first time uploading location
     * @param context Context
     * @return true if this is the first upload, false otherwise
     */
    fun isFirstUpload(context: Context): Boolean {
        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        val firstUploadDone = prefs.getBoolean(KEY_FIRST_UPLOAD, false)
        Log.e("LocationUtils", "Checking if first upload: ${!firstUploadDone}")
        return !firstUploadDone
    }
    
    /**
     * Mark that the first upload has been completed
     * @param context Context
     */
    fun markFirstUploadDone(context: Context) {
        val prefs = context.getSharedPreferences(LOCATION_PREFS, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_FIRST_UPLOAD, true).apply()
        Log.e("LocationUtils", "Marked first upload as done")
    }
    
    /**
     * Check if current location is significantly different from last saved location
     * @param context Context
     * @param currentLat Current latitude
     * @param currentLng Current longitude
     * @param minDistanceMeters Minimum distance in meters to consider significant
     * @return true if distance is greater than minDistanceMeters or no previous location exists
     */
    fun isSignificantLocationChange(
        context: Context, 
        currentLat: Double, 
        currentLng: Double, 
        minDistanceMeters: Float = 50f
    ): Boolean {
        // Check if this is the first upload
        if (isFirstUpload(context)) {
            Log.e("LocationUtils", "First upload detected, considering as significant change")
            return true
        }
        
        val lastLocation = getLastLocationFromPrefs(context)
        
        if (lastLocation == null) {
            Log.e("LocationUtils", "No previous location, considering as significant change")
            return true
        }
        
        val (lastLat, lastLng) = lastLocation
        val distance = calculateDistance(lastLat, lastLng, currentLat, currentLng)
        
        Log.e("LocationUtils", "Distance from last location: $distance meters (threshold: $minDistanceMeters meters)")
        return distance > minDistanceMeters
    }




    suspend fun getMyExactLocation(
        activity: FragmentActivity,
        retryCount: Int = 5,
        delayMs: Long = 500
    ): LatLng? {
        var location: LatLng? = null
        repeat(retryCount) {
            location = withContext(Dispatchers.IO) { tryGetMyExactLocation(activity) }
            if (location != null) return@repeat
            delay(delayMs)
        }
        return location
    }



    suspend fun tryGetMyExactLocation(context: FragmentActivity): LatLng? =
        suspendCancellableCoroutine { continuation ->
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            // Variable to track if the continuation has been resumed
            var isResumed = false

            // Check if location permissions are granted
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {

                // Start the retry logic
                val maxRetryCount = 3
                var retryCount = 0

                // Create a helper function to handle retries
                @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
                fun fetchLocation() {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null && !isResumed) {
                            continuation.resume(LatLng(location.latitude, location.longitude))
                            isResumed = true
                        } else {
                            retryCount++
                            if (retryCount < maxRetryCount) {
                                // Delay 1 second and retry
                                CoroutineScope(Dispatchers.IO).launch {
                                    delay(1000)
                                    fetchLocation() // Retry fetching location
                                }
                            } else if (!isResumed) {
                                continuation.resume(null)
                                isResumed = true
                            }
                        }
                    }.addOnFailureListener { exception ->
                        if (!isResumed) {
                            continuation.resumeWithException(exception)
                            isResumed = true
                        }
                    }
                }

                // Start fetching the location
                fetchLocation()

            } else {
                // Request permissions if not granted
                ActivityCompat.requestPermissions(
                    context,
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    1
                )
                if (!isResumed) {
                    continuation.resume(null)
                    isResumed = true
                }
            }
        }
}
