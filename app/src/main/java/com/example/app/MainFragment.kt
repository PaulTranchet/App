package com.example.app

import android.content.Context.SENSOR_SERVICE
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_GYROSCOPE
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Frame
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.utils.doOnApplyWindowInsets
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class MainFragment : Fragment(R.layout.fragment_main), SensorEventListener {
    lateinit var sceneView: ArSceneView
    lateinit var actionButton: ExtendedFloatingActionButton

    var triggerTakePicture: Boolean = false
    var canTakePicture: Boolean = false

    lateinit var sensorManager: SensorManager
    lateinit var accelerationSensor: Sensor
    lateinit var gyroscopeSensor: Sensor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        InitSensorActivity()

        // Initializing camera
        sceneView = view.findViewById(R.id.sceneView)
        sceneView.onArFrame = {
            takePhoto();
        }
        sceneView.arCameraStream.cameraTexture

        // Initializing interface
        actionButton = view.findViewById<ExtendedFloatingActionButton>(R.id.actionButton).apply {
            val bottomMargin = (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin
            doOnApplyWindowInsets { systemBarsInsets ->
                (layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                    systemBarsInsets.bottom + bottomMargin
            }
            setOnClickListener { setCapturePictureToTrue() }
        }
    }

    override fun onResume() {
        super.onResume()
        sensorManager.registerListener(this, accelerationSensor, SensorManager.SENSOR_DELAY_NORMAL)
        sensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    fun InitSensorActivity() {
        sensorManager = requireActivity().getSystemService(SENSOR_SERVICE) as SensorManager
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscopeSensor = sensorManager.getDefaultSensor(TYPE_GYROSCOPE)
    }


    fun isPhoneNotMoving(accelerations: FloatArray): Boolean {
        for (i in 0..2) {
            // m/s²
            if (abs(accelerations[i]) > 0.01) { // test
                return false
            }
        }
        return true
    }

    fun isPhoneOrientedDown(orientations: FloatArray): Boolean {
        // Conversion from radian to degrees
        for (i in 0..2) {
            orientations[i] = Math.toDegrees(orientations[i].toDouble()).toFloat()
        }

        if (abs(orientations[0]) > 10 || abs(orientations[1]) > 10) {
            return false
        }
        return true
    }

    /*
    * Ask about whether to use cameraX or not.
    * Ask about resolution of Filament
    * */
    fun computeDepthOfField()
    {
        var focalLength: Float = 0f //distance between the lens and the focal point
        var hyperfocalDistance: Float = 0f // first distance at which the infinity is sharp
        var aperture: Float = 0f // f/number, the opening that lets the light enter the camera
        var minimumFocusDistance: Float = 0f // first distance at which the subject is sharp
        var circleOfConfusion: Float = 0f // size of the circle where light rays converge, but are not focused perfectly on a point

        hyperfocalDistance = (focalLength * focalLength) / (aperture * circleOfConfusion)
        var firstSharpDistance = (hyperfocalDistance * minimumFocusDistance) / (hyperfocalDistance + (minimumFocusDistance - focalLength))
        var lastSharpDistance = (hyperfocalDistance * minimumFocusDistance) / (hyperfocalDistance - (minimumFocusDistance - focalLength))
        var depthOfField = lastSharpDistance - firstSharpDistance
    }

    /* Implementing SensorEventListener functions */
    var accelerometerReading = FloatArray(3)
    var gyroscopeReading = FloatArray(3)

    override fun onSensorChanged(event: SensorEvent?) {
        var sensorName = event!!.sensor.name
        if (event == null) {
            //Do nothing
        } else if (event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            //event values are in m/s²
            System.arraycopy(event.values, 0, accelerometerReading, 0, accelerometerReading.size)
            Log.d(
                sensorName,
                ": X: " + event.values[0] + "; Y: " + event.values[1] + "; Z: " + event.values[2]
            )
        } else if (event.sensor.type == Sensor.TYPE_GYROSCOPE) {
            //event values are in rad/s
            System.arraycopy(event.values, 0, gyroscopeReading, 0, gyroscopeReading.size)
        }

        canTakePicture = isPhoneNotMoving(accelerometerReading) && isPhoneOrientedDown(gyroscopeReading)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        //Check if the readings are accurate enough
    }

    /* Photo functions */
    private fun NV21toJPEG(nv21: ByteArray, width: Int, height: Int): ByteArray? {
        val out = ByteArrayOutputStream()
        val yuv = YuvImage(nv21, ImageFormat.NV21, width, height, null)
        yuv.compressToJpeg(Rect(0, 0, width, height), 100, out)
        return out.toByteArray()
    }

    private fun YUV_420_888toNV21(image: Image): ByteArray {
        val nv21: ByteArray
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        nv21 = ByteArray(ySize + uSize + vSize)

        //U and V are swapped
        yBuffer[nv21, 0, ySize]
        vBuffer[nv21, ySize, vSize]
        uBuffer[nv21, ySize + vSize, uSize]
        return nv21
    }

    fun WriteImageInformation(image: Image, path: String?) {
        var data: ByteArray? = null
        data = NV21toJPEG(
            YUV_420_888toNV21(image),
            image.width, image.height
        )
        val bos = BufferedOutputStream(FileOutputStream(path))
        bos.write(data)
        bos.flush()
        bos.close()
    }

    fun setCapturePictureToTrue() {
        this.triggerTakePicture = true;
    }

    fun takePhoto() {
        if (triggerTakePicture == true) {
            try {
                val root: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val filepath: String = root.toString() + File.separator + "test2.jpeg"
                Log.e("TAKE PHOTO", filepath)

                val frame: Frame = sceneView.arSession!!.update()
                var image: Image =
                    frame.acquireCameraImage() //limited to 640x480, but has much higher performances than GLES
                WriteImageInformation(image, filepath)
                image.close()

                var toast: Toast = Toast.makeText(context, "photo taken", Toast.LENGTH_SHORT)
                toast.show()
            } catch (t: Exception) {
                Log.e("TAKE PHOTO", "Exception on the OpenGL thread", t);
            }
            triggerTakePicture = false
        }
    }
}