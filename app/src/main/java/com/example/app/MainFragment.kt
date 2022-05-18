package com.example.app

import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.ar.core.Frame
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.utils.doOnApplyWindowInsets
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream


class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var actionButton: ExtendedFloatingActionButton

    lateinit var sensorManager: SensorManager
    lateinit var accelerationSensor: Sensor
    lateinit var gyroscopeSensor: Sensor

    var capturePicture : Boolean = false

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

    fun setCapturePictureToTrue()
    {
        this.capturePicture = true;
    }

    fun takePhoto()
    {
        if (capturePicture == true) {
            try {
                val root: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val filepath: String = root.toString() + File.separator + "test2.jpeg"
                Log.e("TAKE PHOTO", filepath)

                val frame: Frame = sceneView.arSession!!.update()
                var image: Image = frame.acquireCameraImage() //limited to 640x480, but has much higher performances than GLES
                WriteImageInformation(image, filepath)
                image.close()

                var toast : Toast = Toast.makeText(context, "photo taken", Toast.LENGTH_SHORT)
                toast.show()
            } catch (t: Exception) {
                Log.e("TAKE PHOTO", "Exception on the OpenGL thread", t);
            }
            capturePicture = false
        }
    }

    fun checkOrientation() : Boolean
    {
        val orientations = FloatArray(3)
        // Conversion from radians to degrees
        for (i in 0..2) {
            orientations[i] = Math.toDegrees(orientations[i].toDouble()).toFloat()
        }
        if (orientations[0] > 10 || orientations[0] < -10 || orientations[1] > 10 || orientations[1] < -10)
        {
            return false
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initializing sensors
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

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
}