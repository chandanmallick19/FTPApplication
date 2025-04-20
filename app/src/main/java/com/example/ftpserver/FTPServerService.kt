package com.example.ftpserver

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission
import org.apache.ftpserver.usermanager.impl.TransferRatePermission
import java.io.File

class FTPServerService : Service() {

    private var server: org.apache.ftpserver.FtpServer? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val port = intent?.getIntExtra("PORT", 2221) ?: 2221
        val username = intent?.getStringExtra("USERNAME") ?: "android"
        val password = intent?.getStringExtra("PASSWORD") ?: "password"

        startForeground(1, createNotification())

        startFtpServer(username, password, port)

        return START_STICKY
    }

    private fun startFtpServer(username: String, password: String, port: Int) {
        try {
            val serverFactory = FtpServerFactory()
            val listenerFactory = ListenerFactory().apply {
                setPort(port)
            }

            serverFactory.addListener("default", listenerFactory.createListener())

            val userManager = CustomUserManager()

            // Set home directory
            val homeDirectory: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Scoped storage workaround
                Environment.getExternalStorageDirectory().absolutePath // this gives full storage (though deprecated)
            } else {
                Environment.getExternalStorageDirectory().absolutePath
            }

            // Ensure the directory exists
            val rootDir = File(homeDirectory)
            if (!rootDir.exists()) rootDir.mkdirs()

            // Create a test file
            val testFile = File(rootDir, "testfile.txt")
            if (!testFile.exists()) {
                testFile.writeText("This is a test file for FTP access.")
            }

            val user = BaseUser().apply {
                name = username
                setPassword(password)
                this.homeDirectory = homeDirectory
                setEnabled(true)  // Use setEnabled instead of isEnabled
                maxIdleTime = 0
                authorities = listOf<Authority>(
                    WritePermission(),
                    ConcurrentLoginPermission(10, 5),
                    TransferRatePermission(0, 0) // unlimited
                )
            }

            userManager.save(user)
            serverFactory.userManager = userManager

            server = serverFactory.createServer()
            server?.start()

            Log.i("FTPServerService", "FTP server started on port $port at $homeDirectory")

        } catch (e: Exception) {
            Log.e("FTPServerService", "Error starting FTP server", e)
        }
    }

    private fun createNotification(): Notification {
        val channelId = "ftp_server_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                channelId,
                "FTP Server Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("FTP Server Running")
            .setContentText("FTP server is running in the background.")
            .setSmallIcon(android.R.drawable.stat_sys_upload)  // Use default icon
            .build()
    }


    override fun onDestroy() {
        super.onDestroy()
        server?.stop()
        Log.i("FTPServerService", "FTP server stopped")
    }

    override fun onBind(intent: Intent?) = null
}
