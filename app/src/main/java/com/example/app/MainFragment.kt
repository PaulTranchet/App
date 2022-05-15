package com.example.app

import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.ar.core.Frame
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.ar.ArSceneView
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

typealias Position = Float3
typealias Rotation = Float3

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: ArSceneView

    var capturePicture : Boolean = false

    fun setCapturePictureToTrue()
    {
        this.capturePicture = true;
    }

    fun takePhoto()
    {
        if (capturePicture == true) {
            try {
                val root: File? = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                val frame: Frame = sceneView.arSession!!.update()
                var image: Image = frame.acquireCameraImage()
                val h = image.height
                val w = image.width
                var bitmap: Bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

                val filename: String = root.toString() + File.separator + "test.jpeg"
                Log.e("TAKE PHOTO", filename)
                FileOutputStream(filename).use { out: OutputStream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                }
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
    }
}