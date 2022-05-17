package com.example.app

import android.graphics.*
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
import java.io.*


class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView
    lateinit var actionButton: ExtendedFloatingActionButton

    var capturePicture : Boolean = false

    fun setCapturePictureToTrue()
    {
        this.capturePicture = true;
    }

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

    fun takePhoto()
    {
        if (capturePicture == true) {
            try {
                val root: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val filepath: String = root.toString() + File.separator + "test.jpeg"
                Log.e("TAKE PHOTO", filepath)

                val frame: Frame = sceneView.arSession!!.update()
                var image: Image = frame.acquireCameraImage() //limited to 640x480, but has much higher performances than GLES
                WriteImageInformation(image, filepath)

                var toast : Toast = Toast.makeText(context, "photo taken", Toast.LENGTH_SHORT)
                toast.show()
            } catch (t: Exception) {
                Log.e("TAKE PHOTO", "Exception on the OpenGL thread", t);
            }
            capturePicture = false
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)
        sceneView.onArFrame = {
            takePhoto();
        }

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