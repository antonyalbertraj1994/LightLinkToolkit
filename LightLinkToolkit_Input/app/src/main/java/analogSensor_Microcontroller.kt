package com.example.lightlinktoolkit_study.presentation

import android.app.Activity
import android.content.ContentResolver

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints

class analogSensor_Microcontroller (private val activity: Activity, private val brightness:Int, private val maxvoltage_mic:Double, private val onSensorUpdate: (String) -> Unit ) {
    private var coefficients_scaled = DoubleArray(3)

    var fitting_order_calibration = 1
    private var isTimerRunning:Int = 0
    private lateinit var timer: CountDownTimer
    private var light_array = mutableListOf<Int>()
    private var max_lightval = -1
    private var start_tracking:Boolean = false
    private var predicted_voltage = 0.0
    private val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private var count_no_highvalue = 0
    private var framecount = 0

    fun start_calibration(maxlux:Double) {
        // New fitting
        val points = load_data_microcontroller_3(maxlux)
        var fitter = PolynomialCurveFitter.create(fitting_order_calibration)
        val extremes = getXMinMax(points)
        val min_current = extremes.first
        val max_current = extremes.second

        val coefficients = fitter.fit(points.toList())
        println("Coefficient${coefficients[0]}, ${coefficients[1]}")
        val calibration_current = maxvoltage_mic // 6 for temperature sensor, 100 for microcontroller
        val calibration_lux = maxlux
        val scalefactor = calibration_lux / compute_function(coefficients, calibration_current)
        val current_smooth = linspace(min_current, max_current, 100)
        //computeFunctionArray(coefficients, current_smooth, scalefactor)
        val scaledpoints = computeFunctionAsPoints(coefficients, current_smooth, scalefactor)

        var fitter_scaled = PolynomialCurveFitter.create(fitting_order_calibration)
        coefficients_scaled = fitter_scaled.fit(scaledpoints.toList())
        val lux_value_predict = 4537
        val current_predicted = compute_function(coefficients_scaled, lux_value_predict.toDouble())
        println("scaling_factor:$scalefactor")
        println("CurrentPredicted:$current_predicted")
    }

    fun start() {
        setBrightness(brightness)
        isTimerRunning = 1
        timer = object : CountDownTimer(5000, 1000) {  // 60 sec, interval 1 sec
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                println("Timer:$seconds")
            }

            override fun onFinish() {
                isTimerRunning = 2
                println("Timer Finished!")
            }
        }.start()
    }

    fun computemax_lightval(list:MutableList<Int>):Double {
        println("Computing max1")
        if (list.size < 3) return 0.0  // No local maxima possible in small lists

        val maxima = mutableListOf<Int>()

        // Find local maxima
        for (i in 1 until list.size - 1) {
            if (list[i] > list[i - 1] && list[i] > list[i + 1]) {
                maxima.add(list[i])
            }
        }
        println("Computing max2:${maxima.average()}")

        start_calibration(maxima.average())
        println("Computing max2:${maxima.average()}")
        // Calculate the average
        return if (maxima.isNotEmpty()) {
            maxima.average()
        } else {
            0.0 // Return 0 if no local maxima are found
        }

    }


    fun Sensor(event: SensorEvent):String {
        val lightval = event.values[0]
        val lightvalue = lightval.toInt()
        println("Lightvalue:$lightvalue")

        if(isTimerRunning == 1) {
            light_array.add(lightvalue)
        } else if (isTimerRunning == 2) {
            max_lightval = computemax_lightval(light_array).toInt()
            isTimerRunning = 3
            light_array.clear()
        }

        if(isTimerRunning == 1) {
            return "Calib"
        }

        framecount += 1
        if(isWithinTenPercent(max_lightval, lightvalue, 0.02f) && start_tracking){
            val min_sensorval = light_array.min()
            light_array.clear()

            val predicted_current = compute_function(coefficients_scaled, min_sensorval.toDouble())
            predicted_voltage = predicted_current * 0.001 * 330

            println("Debug_Lightval1:$min_sensorval, minval")
            start_tracking = false
        }

        if(isWithinTenPercent(max_lightval, lightvalue, 0.02f) && !start_tracking){
            start_tracking = true
            count_no_highvalue += 1
        }

        if (framecount % 10 == 0){
            if (count_no_highvalue == 0) {
                start_tracking = false
                isTimerRunning = 1
                max_lightval = -1
                start()
                println("Calibrating Again")
                return "Calib"
            }
            count_no_highvalue = 0
            framecount = 0
        }

        if (start_tracking) {
            light_array.add(lightvalue)
        }


        if (predicted_voltage < 0) {
            return  0.0.toString()
        } else {
            return predicted_voltage.toString()
        }

    }

    fun computeFunctionAsPoints(coeffs: DoubleArray, currentArray: DoubleArray,scale:Double): WeightedObservedPoints {
        val points = WeightedObservedPoints()

        if(fitting_order_calibration == 1){
            currentArray.forEach { current ->
                val lux = (coeffs[0] + coeffs[1] * current)*scale //+ coeffs[2] * current * current)*scale
                println("Current: $current -> Lux: $lux")  // Print each value
                points.add(lux, current)  // Add to WeightedObservedPoints
            }
        } else{
            currentArray.forEach { current ->
                val lux = (coeffs[0] + coeffs[1] * current+ coeffs[2] * current * current)*scale
                println("Current: $current -> Lux: $lux")  // Print each value
                points.add(lux, current)  // Add to WeightedObservedPoints
            }
        }

        return points
    }

    fun isWithinTenPercent(reference: Int, value: Int, percent:Float): Boolean {
        val lowerBound = reference * (1.0 - percent)  // 90% of reference
        val upperBound = reference * (1.0 + percent)  // 110% of reference
        return value.toDouble() in lowerBound..upperBound
    }

    fun compute_function(coeffs:DoubleArray, current:Double):Double {
        if(fitting_order_calibration == 1){
            val computed_lux = coeffs[0] + coeffs[1] * current //+ coeffs[2] * current * current
            return computed_lux
        } else {
            val computed_lux = coeffs[0] + coeffs[1] * current + coeffs[2] * current * current
            return computed_lux
        }
    }


    fun getXMinMax(points: WeightedObservedPoints): Pair<Double, Double> {
        val xValues = points.toList().map { it.x }
        val minX = xValues.minOrNull() ?: Double.NaN
        val maxX = xValues.maxOrNull() ?: Double.NaN
        return minX to maxX
    }

    fun linspace(start: Double, end: Double, num: Int): DoubleArray {
        require(num > 1) { "num must be greater than 1" }

        val step = (end - start) / (num - 1)
        return DoubleArray(num) { i -> start + i * step }
    }


    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event!!.sensor.type == Sensor.TYPE_LIGHT) {
                val sensor_value = Sensor(event)
                println("Sensor listener")
                onSensorUpdate(sensor_value.toString())
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    fun register() {
        lightSensor?.also {
            sensorManager.registerListener(sensorEventListener, it, SensorManager.SENSOR_DELAY_FASTEST)
            println("Sensor Registered")
            // start()
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(sensorEventListener)
        println("Sensor Unregistered")
    }


    fun setBrightness(brightness:Int?) {
        if (brightness == null) return
        val contentResolver: ContentResolver = activity.contentResolver
        Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness!!)

        // Apply the brightness change to the current window
        val layoutParams: WindowManager.LayoutParams = activity.window.attributes
        val brightval = brightness / 100.0f
        Log.d("Brightnessval", "$brightval")
        layoutParams.screenBrightness = brightness / 100.0f
        activity.window.attributes = layoutParams
    }


    fun load_data_microcontroller_3(threshold:Double) : WeightedObservedPoints{
        val points = WeightedObservedPoints()

        // Your data as a multiline string
        val data = """
        0.0 , 350
        0.1 , 415
        0.2 , 511
        0.3 , 605
        0.4 , 668
        0.5 , 759
        0.6 , 857
        0.7 , 949
        0.8 , 1016
        0.9 , 1109
        1.0 , 1211
        1.1 , 1300
        1.2 , 1374
        1.3 , 1473
        1.4 , 1568
        1.5 , 1629
        1.6 , 1738
        1.7 , 1826
        1.8 , 1929
        1.9 , 1991
        2.0 , 2088
        2.1 , 2184
        2.2 , 2292
        2.3 , 2378
        2.4 , 2474
        2.5 , 2577
        2.6 , 2650
        2.7 , 2693
        2.8 , 2819
        2.9 , 2916
        3.0 , 3019
        3.1 , 3083
        3.2 , 3201
        3.3 , 3285
""".trimIndent()

        // Parse and add the points
        data.lines().forEach { line ->
            val (x, y) = line.split(",").map { it.trim().toDouble() }
            //if( y < (threshold)) {
            points.add(x, y)
            //}
        }
        return points
    }




}