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

class AnalogSensor (private val activity: Activity, private val brightness:Int, private val maxcurrent:Double, private val onSensorUpdate: (String) -> Unit ) {
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
        val points = load_data(maxlux)
        var fitter = PolynomialCurveFitter.create(fitting_order_calibration)
        val extremes = getXMinMax(points)
        val min_current = extremes.first
        val max_current = extremes.second

        val coefficients = fitter.fit(points.toList())
        println("Coefficient${coefficients[0]}, ${coefficients[1]}")
        val calibration_current = maxcurrent// 6 for temperature sensor, 100 for microcontroller
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

    fun load_data(threshold:Double): WeightedObservedPoints {
        val points = WeightedObservedPoints()

        // Your data as a multiline string
        val data = """
          0.0 , 0
          0.1 , 53
          0.2 , 169
          0.3 , 308
          0.4 , 455
          0.5 , 592
          0.6 , 776
          0.7 , 948
          0.8 , 1087
          0.9 , 1263
          1.0 , 1434
          1.1 , 1650
          1.2 , 1840
          1.3 , 2014
          1.4 , 2147
          1.5 , 2280
          1.6 , 2413
          1.7 , 2560
          1.8 , 2689
          1.9 , 2841
          2.0 , 3006
          2.1 , 3180
          2.2 , 3340
          2.3 , 3522
          2.4 , 3704
          2.5 , 3880
          2.6 , 4023
          2.7 , 4167
          2.8 , 4240
          2.9 , 4374
          3.0 , 4508
          3.1 , 4721
          3.2 , 4831
          3.3 , 5035
          3.4 , 5110
          3.5 , 5293
          3.6 , 5425
          3.7 , 5607
          3.8 , 5802
          3.9 , 5959
          4.0 , 6069
          4.1 , 6154
          4.2 , 6298
          4.3 , 6417
          4.4 , 6587
          4.5 , 6731
          4.6 , 6825
          4.7 , 6995
          4.8 , 7168
          4.9 , 7316
          5.0 , 7486
          5.1 , 7724
          5.2 , 7774
          5.3 , 7919
          5.4 , 8063
          5.5 , 8157
          5.6 , 8292
          5.7 , 8437
          5.8 , 8597
          5.9 , 8673
          6.0 , 8843
          6.1 , 8804
          6.2 , 8973
          6.3 , 9192
          6.4 , 9308
          6.5 , 9401
          6.6 , 9474
          6.7 , 9574
          6.8 , 9709
          6.9 , 9860
          7.0 , 9995
          7.1 , 10128
          7.2 , 10295
          7.3 , 10431
          7.4 , 10529
          7.5 , 10679
          7.6 , 10839
          7.7 , 10887
          7.8 , 11150
          7.9 , 11148
          8.0 , 11342
          8.1 , 11408
          8.2 , 11534
          8.3 , 11531
          8.4 , 11820
          8.5 , 11944
          8.6 , 12017
          8.7 , 12153
          8.8 , 12328
          8.9 , 12471
          9.0 , 12606
          9.1 , 12749
          9.2 , 12772
          9.3 , 12872
          9.4 , 13008
          9.5 , 13108
          9.6 , 13258
          9.7 , 13393
          9.8 , 13441
          9.9 , 13591
          10.0 , 13803
    """.trimIndent()

        // Parse and add the points
        data.lines().forEach { line ->
            val (x, y) = line.split(",").map { it.trim().toDouble() }
            if( y < (threshold + 100)) {
                points.add(x, y)
            }

        }
        return points
    }


}