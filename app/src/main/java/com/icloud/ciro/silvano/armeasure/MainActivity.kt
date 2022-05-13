package com.icloud.ciro.silvano.armeasure

import android.content.ContentValues.TAG
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.ar.core.SharedCamera
import com.google.ar.core.exceptions.UnavailableException
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var mArButton: Button
    lateinit var txtArEnable:TextView
    lateinit var session:Session
    lateinit var sharedCamera:SharedCamera
    lateinit var cameraId:String
    lateinit var cameraDevice: CameraDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mArButton=findViewById(R.id.enableARbtn)
        txtArEnable=findViewById(R.id.textView)
        //Enable AR-related functionality on ARCore supported devices only.
        maybeEnableArButton()


    }

    override fun onResume() {
        super.onResume()

        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this)
            return
        }
        createSession()

    }
    override fun onDestroy() {
        super.onDestroy()
        session.close()
    }

   fun maybeEnableArButton() {
        val availability = ArCoreApk.getInstance().checkAvailability(this)
        if (availability.isTransient) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            Handler().postDelayed({
                maybeEnableArButton()
            }, 200)
        }
        if (availability.isSupported) {
            mArButton.visibility = View.VISIBLE
            mArButton.isEnabled = true


        } else { // The device is unsupported or unknown.
            mArButton.visibility = View.INVISIBLE
            mArButton.isEnabled = false

        }
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, results: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, results)
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                .show()
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this)
            }
            finish()
        }
    }

    // Verify that ARCore is installed and using the current version.
    fun isARCoreSupportedAndUpToDate(): Boolean {
        return when (ArCoreApk.getInstance().checkAvailability(this)) {
            ArCoreApk.Availability.SUPPORTED_INSTALLED -> true
            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD, ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> {
                try {
                    // Request ARCore installation or update if needed.
                    when (ArCoreApk.getInstance().requestInstall(this, true)) {
                        ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                            Log.i(TAG, "ARCore installation requested.")
                            false
                        }
                        ArCoreApk.InstallStatus.INSTALLED -> true
                    }
                } catch (e: UnavailableException) {
                    Log.e(TAG, "ARCore not installed", e)
                    false
                }
            }

            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE ->
                // This device is not supported for AR.
                false

            ArCoreApk.Availability.UNKNOWN_CHECKING -> {
                // ARCore is checking the availability with a remote query.
                // This function should be called again after waiting 200 ms to determine the query result.
                false
            }
            ArCoreApk.Availability.UNKNOWN_ERROR, ArCoreApk.Availability.UNKNOWN_TIMED_OUT -> {
                // There was an error checking for AR availability. This may be due to the device being offline.
                // Handle the error appropriately.
                false
            }
        }
    }

    //Creazione della Session
    fun createSession(){
        if(isARCoreSupportedAndUpToDate()) {
            session = Session(this, EnumSet.of(Session.Feature.SHARED_CAMERA))
            val config = Config(session)
            session.configure(config)
            sharedCamera = session.sharedCamera
            cameraId = session.cameraConfig.cameraId

            // Wrap the callback in a shared camera callback.
            val wrappedCallback = sharedCamera.createARDeviceStateCallback(cameraDeviceCallback, backgroundHandler)

            // Store a reference to the camera system service.
            val cameraManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager

            // Open the camera device using the ARCore wrapped callback.
            cameraManager.openCamera(cameraId, wrappedCallback, backgroundHandler)
        }
        else{
            throw Exception("ARCore is not supported")
        }

    }






}