package com.example.lightlinktoolkit_output.presentation

import android.app.Activity
import android.content.ContentResolver
import android.graphics.Color
import android.provider.Settings
import android.util.Log
import android.view.Choreographer
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class TransmitData(private val activity: Activity,private val brightness:Int) {
    private var currentBitIndex = 0
    private var frameCount = 0
    private var isFlashing = false

    init {
        setBrightness(brightness)

        println("Intialized")
    }
    private lateinit var update_button: Button
    private val choreographer: Choreographer by lazy {
        Choreographer.getInstance()
    }

    fun startSendingArray(data1: String, textView: TextView, speed:Int):String {
        isFlashing = true
        currentBitIndex = 0  // Reset bit index
        frameCount = 0
        val d1 = "1" + data1 + "0"
        println("Sending + $d1")
        var speed_frames = speed / 16.67
        var mod_speed = speed_frames.toInt()
        val flashRunnable = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isFlashing) {
                    // Toggle colors at 30Hz (every second frame on a 60Hz display)
                    if (frameCount % mod_speed == 0) {
                        textView.setBackgroundColor(
                            if (d1[9 - currentBitIndex] == '0') Color.BLACK else Color.WHITE
                        )

                        // Move to the next bit
                        currentBitIndex++

                        // Stop flashing after displaying all 10 bits
                        if (currentBitIndex >= 10) {
                            isFlashing = false

                            return  // Stop further callbacks
                        }
                    }

                    frameCount++

                    // Keep posting frames until all bits are displayed
                    choreographer.postFrameCallback(this)
                }
            }
        }

        // Start flashing immediately
        choreographer.postFrameCallback(flashRunnable)
        return "Done"
    }


    fun setBrightness(brightness:Int?) {
        val contentResolver: ContentResolver = activity.contentResolver
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness!!)

        // Apply the brightness change to the current window
        val layoutParams: WindowManager.LayoutParams = activity.window.attributes
        val brightval = brightness / 100.0f
        Log.d("Brightnessval", "$brightval")
        layoutParams.screenBrightness = brightness / 100.0f
        activity.window.attributes = layoutParams
    }

}