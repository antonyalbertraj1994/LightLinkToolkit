import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager

class Button(private val activity: Activity,  private val threshold : Int = 500, private val brightness:Int, private val onSensorUpdate: (Pair<Int, Int>) -> Unit ) {

    private val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var lightSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)

    init {
        println("Button Brightness")
        setBrightness(brightness)
    }



    private val sensorEventListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event!!.sensor.type == Sensor.TYPE_LIGHT) {
                val out = Sensor(event)
                println("Sensor listener")
                onSensorUpdate(out)
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    fun setBrightness(brightness: Int?) {
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

    fun Sensor(event: SensorEvent): Pair<Int,Int> {
        val lightval = event.values[0]
        val lightvalue = lightval.toInt()
        println("Lightvalue:$lightvalue")


        if (lightvalue > threshold) {
            return Pair(1, lightvalue)
        } else {
            return Pair(0, lightvalue)
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

}

