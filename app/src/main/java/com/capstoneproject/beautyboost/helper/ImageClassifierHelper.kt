package com.capstoneproject.beautyboost.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ImageClassifierHelper(
    var modelName: String = "acneModel.tflite",
    var threshold: Float = 0.1f,
    val context: Context,
    var maxResults: Int = 5,
    val classifierListener: ClassifierListener? = null
) {

    private var imageClassifier: ImageClassifier? = null

    init {
        setupImageClassifier()
    }

    private fun setupImageClassifier() {
        try {
            val modelsDir = File(context.cacheDir, "models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }
            val modelFile = File(modelsDir, modelName)

            if (!modelFile.exists()) {
                copyModelFromAssets(modelName, modelFile)
            }

            val baseOptions = BaseOptions.builder()
                .setNumThreads(4)
                .build()

            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setScoreThreshold(threshold)
                .setMaxResults(maxResults)
                .setBaseOptions(baseOptions)
                .build()

            imageClassifier = ImageClassifier.createFromFileAndOptions(modelFile, options)
        } catch (e: IOException) {
            classifierListener?.onError("Initialization failed: ${e.message}")
            Log.e(TAG, "Error initializing image classifier", e)
        }
    }

    private fun copyModelFromAssets(modelName: String, modelFile: File) {
        try {
            context.assets.open(modelName).use { inputStream ->
                FileOutputStream(modelFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            throw IOException("Error copying model from assets", e)
        }
    }

    fun classifyStaticImage(imageUri: Uri) {
        val startTime = System.currentTimeMillis()

        try {
            if (imageClassifier == null) {
                setupImageClassifier()
            }

            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(imageUri))

            // Resize the bitmap to 224x224 as required by the model
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)
            val tensorImage = TensorImage.fromBitmap(resizedBitmap)

            val classifications = imageClassifier?.classify(tensorImage)

            val endTime = System.currentTimeMillis()
            val inferenceTime = endTime - startTime

            if (!classifications.isNullOrEmpty()) {
                classifierListener?.onResults(classifications, inferenceTime)
            } else {
                classifierListener?.onError("No classification results")
            }
        } catch (e: Exception) {
            classifierListener?.onError("Error occurred while classifying image: ${e.message}")
            Log.e(TAG, "Error occurred while classifying image", e)
        }
    }

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(results: List<Classifications>?, inferenceTime: Long)
    }

    companion object {
        private const val TAG = "ImageClassifierHelper"
    }
}
