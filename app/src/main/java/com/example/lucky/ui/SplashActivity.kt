package com.example.lucky.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.lucky.LocationUtils
import com.example.lucky.LoginActivity
import com.example.lucky.NewUtils.encrypt
import com.example.lucky.NewUtils.showToast
import com.example.lucky.NewUtils.loadPrivateKeyFromRaw
import com.example.lucky.NewUtils.loadPublicKeyFromRaw
import com.example.lucky.NewUtils.private_key
import com.example.lucky.NewUtils.public_key
import com.example.lucky.R
import com.example.lucky.SimUtils
import com.example.lucky.databinding.ActivitySplashBinding
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.security.PrivateKey
import java.security.PublicKey
import java.util.UUID

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>
    private var permissionsGranted = false
    private var isNavigating = false
    private var obtainedNumbers = mutableListOf<String>()
    private val allowedNumbers = mutableListOf("+923016826762")
    private val handler = Handler(Looper.getMainLooper())
    private val splashDelay = 1500L // 1.5 seconds

    companion object {
        private const val TAG = "SplashActivity"
        private const val PREFS_NAME = "permissions"
        private const val KEY_FIRST_TIME = "first_time_permissions"
    }
    var validation = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            private_key = loadPrivateKeyFromRaw(this, R.raw.private_key)
            public_key = loadPublicKeyFromRaw(this, R.raw.public_key)
        } catch (e: Exception) {
            Log.e("Member", "Failed to load private key", e)
        }


        setupPermissionLauncher()
        checkAndRequestPermissions()


        // Schedule navigation after minimum splash time
        handler.postDelayed({
            if (permissionsGranted && !isNavigating) {
                validateAndProceed()
            }
        }, splashDelay)
    }

    override fun onResume() {
        super.onResume()
        // Check if permissions were granted in settings
        if (!permissionsGranted && !isFirstTimeAskingPermission()) {
            checkAndRequestPermissions()
        }
    }

    private fun setupPermissionLauncher() {
        Log.d(TAG, "Setting up permission launcher")
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            Log.d(TAG, "Permission results: $permissions")

            val hasRequiredPermissions = areRequiredPermissionsGranted()

            if (hasRequiredPermissions) {
                Log.d(TAG, "All required permissions granted")
                permissionsGranted = true
                validateAndProceed()
            } else {
                handlePermissionDenial()
            }
        }
    }

    private fun checkAndRequestPermissions() {
        Log.d(TAG, "Checking permissions")
        val requiredPermissions = getRequiredPermissions()

        if (areRequiredPermissionsGranted()) {
            Log.d(TAG, "All permissions already granted")
            permissionsGranted = true
            validateAndProceed()
            return
        }

        if (isFirstTimeAskingPermission()) {
            Log.d(TAG, "First time asking for permissions")
            setFirstTimeAskingPermission(false)
            permissionLauncher.launch(requiredPermissions)
        } else if (shouldShowRationale()) {
            Log.d(TAG, "Showing permission rationale")
            showPermissionRationaleDialog()
        } else {
            Log.d(TAG, "Permissions previously denied, showing settings dialog")
            showSettingsDialog()
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        permissions.add(Manifest.permission.READ_PHONE_STATE)
        permissions.add(Manifest.permission.READ_PHONE_NUMBERS)

        return permissions.toTypedArray()
    }

    private fun areRequiredPermissionsGranted(): Boolean {
        val fineLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation =
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasLocation = fineLocation || coarseLocation

        val phoneState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        } else true

        val phoneNumbers = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED
        } else true

        Log.d(TAG, "Permission status: Location=$hasLocation, PhoneState=$phoneState, PhoneNumbers=$phoneNumbers")
        return hasLocation && phoneState && phoneNumbers
    }

    private fun shouldShowRationale(): Boolean {
        return shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) ||
                shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_STATE)) ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                        shouldShowRequestPermissionRationale(Manifest.permission.READ_PHONE_NUMBERS))
    }

    private fun validateAndProceed() {
        if (isNavigating) return

        // Check SIM numbers if permission available
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            obtainedNumbers = SimUtils(this).getSimNumbers()
            if (validation){
            showToast("Obtained SIM numbers: $obtainedNumbers" , Toast.LENGTH_LONG)
                if (obtainedNumbers.isEmpty()) {
                    showToast("SIM numbers are not getting" , Toast.LENGTH_LONG)
                }
                else if (obtainedNumbers.none { it in allowedNumbers }) {
                    showToast("SIM numbers not registered in admin" , Toast.LENGTH_LONG)
                    finish()
                    return
                }
            }

        }

        // Proceed with location upload and navigation
        uploadLocationData()
    }

    private fun uploadLocationData() {
        if (!areRequiredPermissionsGranted()) {
            Log.e(TAG, "Attempted to upload location without permissions")
            proceedToLoginScreen()
            return
        }

        lifecycleScope.launch {
            try {
                val latLng = LocationUtils.getMyExactLocation(this@SplashActivity)
                if (latLng != null) {
                    val isSignificantChange = LocationUtils.isSignificantLocationChange(
                        this@SplashActivity,
                        latLng.latitude,
                        latLng.longitude,
                        50f
                    )

                    if (isSignificantChange) {
                        val now = System.currentTimeMillis()
                        // Encrypt fields with RSA-OAEP(SHA-256) using app public key; falls back to plaintext if key missing
                        val payload: Any = mapOf(
                            "lat" to "${latLng.latitude}".encrypt(),
                            "lng" to ("${latLng.longitude}".encrypt()),
                            "timestamp" to ("$now".encrypt())
                        )

                        val db = FirebaseFirestore.getInstance()
                        val newLocationKey = UUID.randomUUID().toString()
                        db.collection("owners").document(MainActivity.Companion.owner)
                            .collection("location").document(newLocationKey)
                            .set(payload)
                            .addOnSuccessListener {
                                LocationUtils.saveLocationToPrefs(this@SplashActivity, latLng.latitude, latLng.longitude)
                                if (LocationUtils.isFirstUpload(this@SplashActivity)) {
                                    LocationUtils.markFirstUploadDone(this@SplashActivity)
                                }
                                Log.d(TAG, "Location uploaded successfully")
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Location upload failed: ${e.message}")
                                showToast("Location upload failed")
                            }
                    } else {
                        LocationUtils.saveLocationToPrefs(this@SplashActivity, latLng.latitude, latLng.longitude)
                    }
                } else {
                    Log.e(TAG, "Failed to get location")
                    showToast("Failed to get location")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in location process: ${e.message}")
                showToast("Location error")
            } finally {
                delay(1000)
                proceedToLoginScreen()
            }
        }
    }

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("This app requires location and phone state permissions to function properly.")
            .setPositiveButton("Grant Permissions") { _, _ ->
                permissionLauncher.launch(getRequiredPermissions())
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Please enable all required permissions in app settings.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", packageName, null)
                }
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Exit") { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun handlePermissionDenial() {
        if (shouldShowRationale()) {
            showPermissionRationaleDialog()
        } else {
            showSettingsDialog()
        }
    }

    private fun proceedToLoginScreen() {
        if (isNavigating) return
        isNavigating = true
        Log.d(TAG, "Navigating to LoginActivity")
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    private fun isFirstTimeAskingPermission(): Boolean {
        return getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getBoolean(KEY_FIRST_TIME, true)
    }

    private fun setFirstTimeAskingPermission(isFirstTime: Boolean) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FIRST_TIME, isFirstTime)
            .apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }



}

