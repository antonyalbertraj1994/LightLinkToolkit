/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.lightlinktoolkit_output.presentation

import android.content.ContentResolver
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.lightlinktoolkit_output.R
import com.example.lightlinktoolkit_output.presentation.theme.LightLinkToolkit_OutputTheme


class MainActivity : ComponentActivity() {
    private var isFlashing = false
    private lateinit var dataflashingpixel: TextView
    private var currentBitIndex = 0
    private var frameCount = 0
    private lateinit var update_button: Button
    var TransmitData = TransmitData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        dataflashingpixel = findViewById(R.id.datapixel)
        dataflashingpixel.setBackgroundColor(Color.WHITE)

        update_button = findViewById(R.id.update)
        update_button.setOnClickListener {
            println("Update button pressed")
            val sendstring = "10101010" // place the 8 bit data to be sent

            TransmitData.startSendingArray(sendstring,dataflashingpixel, 2) // Speed parameters determine the bit length. Value of '1' corresponds to bit length 16.67ms.
            // Higher the value of speed, longer the bit length and slower communication
        }

        setBrightness(100)
    }

    fun setBrightness(brightness:Int?) {
        val contentResolver: ContentResolver = contentResolver
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness!!)

        // Apply the brightness change to the current window
        val layoutParams: WindowManager.LayoutParams = window.attributes
        val brightval = brightness / 100.0f
        Log.d("Brightnessval", "$brightval")
        layoutParams.screenBrightness = brightness / 100.0f
        window.attributes = layoutParams
    }



}

