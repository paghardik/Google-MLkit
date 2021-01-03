package com.mlkit.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.github.florent37.runtimepermission.kotlin.askPermission
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.mlkit.barcode.databinding.ActivityTextRecognizerBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TextRecognizerActivity : AppCompatActivity() {
    private var _binging: ActivityTextRecognizerBinding? = null
    private val binding: ActivityTextRecognizerBinding get() = _binging!!


    val recognizerClient = TextRecognition.getClient()

    private val textRecognizerExecutor: ExecutorService = Executors.newFixedThreadPool(
        Runtime.getRuntime().availableProcessors()
    )

    @SuppressLint("UnsafeExperimentalUsageError")
    private val imageAnalyzer : ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { img ->
        val mediaImage = img.image
        if (mediaImage != null) {


            val inputImage = InputImage.fromMediaImage(mediaImage, img.imageInfo.rotationDegrees)


           /* val result = recognizerClient.process(inputImage)
                .addOnSuccessListener { visionText ->
                    Log.d(TAG, "----------Start---------")
                    Log.d(TAG, visionText.text)
                    val resultText = visionText.text
                    for (block in visionText.textBlocks) {
                        val blockText = block.text
                        val blockCornerPoints = block.cornerPoints
                        val blockFrame = block.boundingBox
                        for (line in block.lines) {
                            val lineText = line.text
                            val lineCornerPoints = line.cornerPoints
                            val lineFrame = line.boundingBox
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementCornerPoints = element.cornerPoints
                                val elementFrame = element.boundingBox
                            }
                        }
                    }

                    Log.d(TAG, "-------End------------")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG," Error Message", e)
                }*/

            // Wait until ImageAnalysis complete
            Tasks.await( recognizerClient.process(inputImage)
                .addOnSuccessListener { visionText ->
                    Log.d(TAG, "----------Start---------")
                    Log.d(TAG, visionText.text)

                   if (visionText.text.isNotEmpty()){
                       val intent = Intent()
                       intent.putExtra("TEXT", visionText.text)
                       setResult(RESULT_OK, intent)
                       finish()
                   }



                    val resultText = visionText.text
                    for (block in visionText.textBlocks) {
                        val blockText = block.text
                        val blockCornerPoints = block.cornerPoints
                        val blockFrame = block.boundingBox
                        for (line in block.lines) {
                            val lineText = line.text
                            val lineCornerPoints = line.cornerPoints
                            val lineFrame = line.boundingBox
                            for (element in line.elements) {
                                val elementText = element.text
                                val elementCornerPoints = element.cornerPoints
                                val elementFrame = element.boundingBox
                            }
                        }
                    }

                    Log.d(TAG, "-------End------------")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG," Error Message", e)
                })
        }
        // After image ImageAnalysis must be closed
        img.close()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binging = ActivityTextRecognizerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.v(BarcodeActivity.TAG, "Number of cores ${Runtime.getRuntime().availableProcessors()}")
        askPermission(*REQUIRED_PERMISSION) {
            startCameraPreview()
        }.onDeclined {
            // Handle denied permissions here
        }
    }

    private fun startCameraPreview() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this@TextRecognizerActivity)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()

                // Camera Preview Setup
                val cameraPreview = Preview.Builder()
                    .build()
                    .also { previewBuilder ->
                        previewBuilder.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
                    }

                // Preview frame analysis
                val imageAnalysis = ImageAnalysis.Builder()
                    .setImageQueueDepth(1)
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(textRecognizerExecutor, imageAnalyzer)

                // Hook every thing in camera preview
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this@TextRecognizerActivity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    imageAnalysis,
                    cameraPreview
                )
            },
            ContextCompat.getMainExecutor(this@TextRecognizerActivity)
        )
    }

    override fun onDestroy() {
        if (!textRecognizerExecutor.isShutdown) {
            textRecognizerExecutor.shutdown()
        }
        super.onDestroy()
    }

    companion object {
        const val TAG = "TextRecognizerActivity"
        private val REQUIRED_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    }
}