package com.example.lightlinktoolkit_output.presentation

import android.graphics.Color
import android.view.Choreographer
import android.widget.Button
import android.widget.TextView

class TransmitData {
    private var currentBitIndex = 0
    private var frameCount = 0
    private var isFlashing = false

    private lateinit var update_button: Button
    private val choreographer: Choreographer by lazy {
        Choreographer.getInstance()
    }

    fun startSendingArray(data1: String, textView: TextView, speed:Int) {
        isFlashing = true
        currentBitIndex = 0  // Reset bit index
        frameCount = 0
        val d1 = "1" + data1 + "0"
        println("Sending + $d1")

        val flashRunnable = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (isFlashing) {
                    // Toggle colors at 30Hz (every second frame on a 60Hz display)
                    if (frameCount % speed == 0) {
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
    }

}