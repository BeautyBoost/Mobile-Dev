package com.capstoneproject.beautyboost.ui

import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.capstoneproject.beautyboost.R

class ResultActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val resultImage: ImageView = findViewById(R.id.result_image)
        val resultsContainer: LinearLayout = findViewById(R.id.results_container)

        // Get data from intent
        val imageUri = intent.getParcelableExtra<Uri>("image_uri")
        val resultText = intent.getStringExtra("result_text")

        // Load image
        imageUri?.let {
            val inputStream = contentResolver.openInputStream(it)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            resultImage.setImageBitmap(bitmap)
            inputStream?.close()
        }

        // Parse result text and add TextViews dynamically
        resultText?.let {
            val results = it.split("\n")
            for (result in results) {
                if (result.isNotBlank()) {
                    val textView = TextView(this).apply {
                        text = result
                        textSize = 18f
                        setTextColor(resources.getColor(R.color.black, null))
                        setPadding(0, 8, 0, 8)
                    }
                    resultsContainer.addView(textView)
                }
            }
        }
    }
}
