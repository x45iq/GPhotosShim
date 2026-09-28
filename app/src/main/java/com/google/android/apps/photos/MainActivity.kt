package com.google.android.apps.photos

import android.app.Activity
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.Log

class MainActivity : Activity() {
    private var cameraRelaunchIntent: PendingIntent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receivedIntent = this.intent

        // Extract all necessary information from the intent
        val sessionId = receivedIntent.getStringExtra("external_session_id")
        val data = receivedIntent.data
        val type = receivedIntent.type
        val processingURI = receivedIntent.parcelableExtra<Uri>("processing_uri_intent_extra")
        cameraRelaunchIntent =
            receivedIntent.parcelableExtra<PendingIntent>("CAMERA_RELAUNCH_INTENT_EXTRA")

        // Make sure there is even a session id, the call is probably invalid if this is gone
        if (sessionId == null || data == null) return finish()

        Log.i(
            TAG, "SessionId: '" + sessionId +
                    "', URI:'" + data +
                    "', type: '" + type +
                    "', processingURI: '" + processingURI + "'"
        )

        // Immediately hand the image over to whatever gallery the user prefers.
        // We do not wait for GCam to finish processing the image (IS_PENDING),
        // so the gallery opens right away instead of after a delay.
        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(data, "image/*")
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "No gallery app found to view the image", e)
            return quit()
        }
        finish()
    }

    // Try to relaunch the camera and then quit this app
    private fun quit() {
        try {
            cameraRelaunchIntent?.send()
        } catch (e: Exception) {
            Log.e(TAG, "Error sending intent", e)
        }
        finish()
    }

    companion object {
        private const val TAG = "PhotosShim"

        // Intent.getParcelableExtra(String) is deprecated since API 33,
        // use the type-safe variant where available
        private inline fun <reified T : Parcelable> Intent.parcelableExtra(name: String): T? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getParcelableExtra(name, T::class.java)
            } else {
                @Suppress("DEPRECATION")
                getParcelableExtra(name) as? T
            }
    }
}
