package com.example.ftpserver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.*
import org.apache.ftpserver.filesystem.nativefs.NativeFileSystemFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission
import org.apache.ftpserver.usermanager.impl.TransferRatePermission
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

class FTPServerService : Service() {

    companion object {
        private const val TAG = "FTPServerService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ftp_server_channel"
        private const val DEFAULT_PORT = 2221
    }

    private var ftpServer: FtpServer? = null
    private var currentPort = DEFAULT_PORT

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        // Start foreground service immediately
        startForeground(NOTIFICATION_ID, createNotification(currentPort))

        return try {
            currentPort = intent?.getIntExtra("PORT", DEFAULT_PORT) ?: DEFAULT_PORT

            if (!verifyStorageAccess()) {
                Log.e(TAG, "Storage access verification failed")
                stopSelf()
                return START_NOT_STICKY
            }

            startFtpServer(currentPort)
            Log.i(TAG, "FTP server started on port $currentPort")
            START_STICKY

        } catch (e: Exception) {
            Log.e(TAG, "FTP server startup failed", e)
            stopSelf()
            START_NOT_STICKY
        }
    }


    private fun verifyStorageAccess(): Boolean {
        return try {
            val testDir = File(Environment.getExternalStorageDirectory(), "ftp_test")
            if (!testDir.exists()) {
                testDir.mkdirs()
            }

            val testFile = File(testDir, "permission_test.txt")
            testFile.writeText("test")
            testFile.delete()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Storage access verification failed", e)
            false
        }
    }

    private fun startFtpServer(port: Int) {
        val serverFactory = FtpServerFactory().apply {
            fileSystem = NativeFileSystemFactory()
            addListener("default", ListenerFactory().apply {
                setPort(port)
            }.createListener())
            userManager = createUserManager()
        }

        ftpServer = serverFactory.createServer().apply {
            start()
        }
    }

    private fun createUserManager(): UserManager {
        val userManagerFactory = PropertiesUserManagerFactory().apply {
            passwordEncryptor = ClearTextPasswordEncryptor()
            // Ensure the directory and file exist
            val userFile = File(filesDir, "ftpusers.properties")
            if (!userFile.exists()) {
                userFile.createNewFile() // Create the file if it doesn't exist
            }
            file = userFile
            adminName = "admin"
        }

        val userManager = userManagerFactory.createUserManager()
        userManager.save(createFtpUser()) // Save the user into the user manager
        return userManager
    }


    private fun createFtpUser(): User {
        val user = BaseUser()
        user.setName("android")
        user.setPassword("password")
        user.setHomeDirectory(Environment.getExternalStorageDirectory().absolutePath)
        user.setEnabled(true)
        user.setMaxIdleTime(0)

        val authorities = listOf<Authority>(
            WritePermission(),
            ConcurrentLoginPermission(10, 5),
            TransferRatePermission(1024 * 1024, 1024 * 1024)
        )

        user.setAuthorities(authorities)
        return user
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel(
                CHANNEL_ID,
                "FTP Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "FTP server is running"
                getSystemService(NotificationManager::class.java)
                    .createNotificationChannel(this)
            }
        }
    }

    private fun createNotification(port: Int): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FTP Server (Port: $port)")
            .setContentText("File transfers enabled")
            .setSmallIcon(R.drawable.ic_ftp_server)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopFtpServer()
        Log.d(TAG, "Service destroyed")
    }

    private fun stopFtpServer() {
        try {
            ftpServer?.takeUnless { it.isStopped }?.stop()
            Log.i(TAG, "FTP server stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping FTP server", e)
        }
    }
}
