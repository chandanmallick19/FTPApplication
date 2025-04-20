# FTP Server for Android

\`\`\`plaintext
A lightweight FTP server implementation for Android devices that allows file transfer between your phone and computer over the same WiFi network.
\`\`\`

## Features

\`\`\`diff
+ 📁 Access phone files from any FTP client
+ 🔒 Basic username/password authentication 
+ 🔄 Supports file uploads and downloads
+ 🌐 Works on local WiFi networks
+ 🔔 Persistent notification while server is running
+ ⚡ Auto-detects device IP address
\`\`\`

## Installation

```bash
# Clone the repository
git clone https://github.com/chandan19/FTPApplication.git

# Open in Android Studio
1. File → Open → Select project directory
2. Build and run on your Android device (API 21+)
'''

## Usage

'''
1. Launch the app on your Android device
2. Connect both devices to same WiFi network
3. Note the IP and port displayed
4. Connect via FTP client:
   Host: [Android IP]
   Port: 2221
   Username: android
   Password: password
'''

## Configuration

'''
// FTPServerService.kt
private fun createFtpUser(): User {
    return object : User {
        override fun getName() = "custom_username" // EDIT THIS
        override fun getPassword() = "secure_password" // EDIT THIS
        // ...
    }
}
'''

'''
// MainActivity.kt
private val ftpPort = 2121 // CHANGE PORT HERE
'''

## Permissions

'''
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET"/>
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
'''

## Dependencies

'''
// build.gradle
dependencies {
    implementation 'org.apache.ftpserver:ftpserver-core:1.1.1'
    implementation 'org.apache.mina:mina-core:2.1.5'
    implementation 'androidx.core:core-ktx:1.12.0'
}
'''
## Troubleshooting

'''
# Connection issues?
1. ping [Android IP] # Verify connectivity
2. telnet [Android IP] 2221 # Test port
3. Check firewall settings
'''

## License

'''
MIT License
Copyright (c) 2025 [Chandan Kumar Mallick]
Full license text at: LICENSE.md
'''

## Contact

'''
{
  "email": "chandansoumya28@gmail.com",
  "github": "https://github.com/chandan19",
  "issues": "https://github.comchandan19/FTPApplication/issues"
}
'''
