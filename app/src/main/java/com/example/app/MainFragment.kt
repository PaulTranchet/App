package com.example.app

import android.os.Bundle
import android.view.View
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.filament.utils.HDRLoader
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.SceneView
import io.github.sceneview.environment.loadEnvironment

typealias Position = Float3
typealias Rotation = Float3

class MainFragment : Fragment(R.layout.fragment_main) {

    lateinit var sceneView: SceneView
    lateinit var loadingView: View


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sceneView = view.findViewById(R.id.sceneView)
        loadingView = view.findViewById(R.id.loadingView)

        sceneView.camera.position = Position(x = 4.0f, y = -1.0f)
        sceneView.camera.rotation = Rotation(x = 0.0f, y = 80.0f)

        lifecycleScope.launchWhenCreated {
            sceneView.camera.smooth(
                position = Position(x = -1.0f, y = 1.5f, z = -3.5f),
                rotation = Rotation(x = -60.0f, y = -50.0f),
                speed = 0.5f
            )
        }
    }
}