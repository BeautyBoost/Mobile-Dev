package com.capstoneproject.beautyboost.ui

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.capstoneproject.beautyboost.databinding.ActivityScanBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import com.capstoneproject.beautyboost.helper.ImageClassifierHelper
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.io.InputStream
import java.text.NumberFormat

class ScanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanBinding
    private lateinit var previewView: PreviewView
    private lateinit var imageView: ImageView
    private lateinit var buttonCapture: ImageButton
    private lateinit var buttonGallery: ImageButton
    private lateinit var buttonSwitchCamera: ImageButton
    private lateinit var buttonAnalyze: Button

    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private lateinit var photoFile: File
    private lateinit var bitmap: Bitmap

    private val PICK_IMAGE_REQUEST = 1
    private var imageUri: Uri? = null

    val auth = Firebase.auth
    val user = auth.currentUser

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                displayImage(it)
            }
        }

    private val REQUEST_CODE_PERMISSIONS = 10
    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        previewView = binding.previewView
        imageView = binding.imageView
        buttonCapture = binding.buttonCapture
        buttonGallery = binding.buttonGallery
        buttonSwitchCamera = binding.buttonSwitchCamera
        buttonAnalyze = binding.submitImage

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        buttonCapture.setOnClickListener { takePhoto() }
        buttonGallery.setOnClickListener { openGallery() }
        buttonSwitchCamera.setOnClickListener { switchCamera() }
        buttonAnalyze.setOnClickListener {
            imageUri?.let {
                analyzeImage(it)
            } ?: run {
                showToast("Silahkan Memilih Gambar Agar Dapat Diproses")
            }
        }


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                // Handle exceptions
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        photoFile = File(
            externalMediaDirs.firstOrNull(),
            "${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Handle the error
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    displayImage(Uri.fromFile(photoFile))
                }
            }
        )
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun switchCamera() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera()
    }

    private fun displayImage(uri: Uri) {
        previewView.visibility = View.GONE
        imageView.setImageURI(uri)
        imageView.visibility = View.VISIBLE
        imageUri = uri

        // Convert URI to Bitmap
        val inputStream: InputStream? = contentResolver.openInputStream(uri)
        bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun analyzeImage(uri: Uri) {
        val imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        showToast(error)
                    }
                }

                override fun onResults(results: List<Classifications>?, inferenceTime: Long) {
                    runOnUiThread {
                        if (results.isNullOrEmpty() || results[0].categories.isEmpty()) {
                            showToast("Pilih gambar yang ingin diproses")
                        } else {
                            val stringBuilder = StringBuilder()
                            for (classification in results) {
                                for (category in classification.categories) {
                                    val detectionLabel = category.label
                                    stringBuilder.append("$detectionLabel\n")
                                }
                            }
                            moveToResult(uri, stringBuilder.toString().trim())
                        }
                    }
                }
            }
        )

        imageClassifierHelper.classifyStaticImage(uri)
    }


    private fun moveToResult(uri: Uri, result: String) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("image_uri", uri)
            putExtra("result_text", result)
        }
        startActivity(intent)
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
