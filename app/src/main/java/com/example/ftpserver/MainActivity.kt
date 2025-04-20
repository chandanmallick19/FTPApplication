package com.example.ftpserver

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.ftpserver.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isServerRunning = false
    private val ftpPort = 2221
    private val STORAGE_PERMISSION_CODE = 1001
    private val MANAGE_STORAGE_CODE = 1002

    // Contract for MANAGE_EXTERNAL_STORAGE permission
    private val manageStorageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            Environment.isExternalStorageManager()) {
            startServer()
        } else {
            showToast("Storage permission required to start server")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        displayIpAddress()
    }

    private fun setupUI() {
        binding.apply {
            startButton.setOnClickListener {
                if (checkAndRequestPermissions()) {
                    startServer()
                }
            }

            stopButton.setOnClickListener {
                stopServer()
            }

            portText.text = "Port: $ftpPort"
            updateServerUI(false)
        }
    }

    private fun displayIpAddress() {
        try {
            val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
            val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            binding.ipAddressText.text = "IP: $ipAddress"
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to get IP address", e)
            binding.ipAddressText.text = "IP: Unavailable"
        }
    }

    private fun startServer() {
        if (isServerRunning) return

        try {
            val username = binding.usernameEditText.text?.toString()?.ifBlank { "android" } ?: "android"
            val password = binding.passwordEditText.text?.toString()?.ifBlank { "password" } ?: "password"

            val serviceIntent = Intent(this, FTPServerService::class.java).apply {
                putExtra("PORT", ftpPort)
                putExtra("USERNAME", username)
                putExtra("PASSWORD", password)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }

            isServerRunning = true
            updateServerUI(true)
            showToast("FTP server started on port $ftpPort")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to start server", e)
            showToast("Failed to start server: ${e.localizedMessage}")
            updateServerUI(false)
        }
    }

    private fun stopServer() {
        if (!isServerRunning) return

        try {
            stopService(Intent(this, FTPServerService::class.java))
            isServerRunning = false
            updateServerUI(false)
            showToast("FTP server stopped")
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to stop server", e)
            showToast("Failed to stop server: ${e.localizedMessage}")
        }
    }

    private fun updateServerUI(isRunning: Boolean) {
        binding.apply {
            statusText.text = if (isRunning) "Status: Running" else "Status: Stopped"
            startButton.isEnabled = !isRunning
            stopButton.isEnabled = isRunning
        }
    }

    private fun checkAndRequestPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                true
            } else {
                showStoragePermissionExplanation()
                false
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> true

                ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) -> {
                    showStoragePermissionExplanation()
                    false
                }

                else -> {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_CODE
                    )
                    false
                }
            }
        }
    }

    private fun showStoragePermissionExplanation() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Needed")
            .setMessage("The app needs access to your storage to function as an FTP server")
            .setPositiveButton("Grant") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        manageStorageLauncher.launch(intent)
                    } catch (e: Exception) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        manageStorageLauncher.launch(intent)
                    }
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                        STORAGE_PERMISSION_CODE
                    )
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            STORAGE_PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startServer()
                } else {
                    showToast("Storage permission denied")
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isServerRunning) {
            stopServer()
        }
    }
}
