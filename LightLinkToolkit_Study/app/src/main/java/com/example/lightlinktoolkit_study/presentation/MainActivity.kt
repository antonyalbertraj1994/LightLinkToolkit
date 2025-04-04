/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.lightlinktoolkit_study.presentation

import Button
import AnalogSensor
import android.content.ContentResolver
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.health.connect.datatypes.units.Temperature
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.lightlinktoolkit_study.R

class MainActivity : ComponentActivity() {
    private var lightSensor: Sensor? = null
    private lateinit var sensorManager: SensorManager

    private lateinit var sensorValue_TextView: TextView

    var experimentTool_ID = 2 // 1 for button , and 2 for analog sensor
    private lateinit var analogSensor:AnalogSensor
    private lateinit var buttonSensor:Button

    //var Button = Button()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Textview to see the values
        sensorValue_TextView = findViewById(R.id.sensor_value)
        sensorValue_TextView.setTextColor(Color.WHITE)



        //Analog Sensor
        analogSensor = AnalogSensor(this, 0, ::onAnalogValueChanged)  // Pass the reference of the main activity, and screen brightness
        analogSensor.start()

        //One button
        //buttonSensor = Button(this, 500, 50, ::onButtonValueChanged)  //Reference of this class, threshold for detecting switch, screen brightness, callback for reading button status
    }

    private fun onButtonValueChanged(value:Pair<Int, Int> ) {
        println("Button Value:$value")
        sensorValue_TextView.text = value.first.toString()
    }

    private fun onAnalogValueChanged(value:String ) {
        println("Button Value:$value")
        sensorValue_TextView.text = value
    }

    override fun onResume() {
        super.onResume()
        analogSensor.register()

    }

    override fun onPause() {
        super.onPause()
        analogSensor.unregister()
    }




}

