package com.capstoneproject.beautyboost.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capstoneproject.beautyboost.R
import com.capstoneproject.beautyboost.databinding.ActivityHomeBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class HomeActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private var auth = Firebase.auth
    val user = auth.currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (user != null) {
            val userName = user.displayName?.split(" ")?.get(0) ?: "User"
            binding.name.text = "Selamat Datang, " + userName
        } else {
            // Handle the case when the user is not signed in
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        binding.analyzeButton.root.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }

        binding.logoutButton.setOnClickListener {
            signOut()
        }

        // Set up RecyclerView
        val articlesAdapter = ArticlesAdapter(getArticlesList())
        binding.articlesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.articlesRecyclerView.adapter = articlesAdapter
    }

    private fun getArticlesList(): List<Article> {
        // Dummy data, replace with real data fetching logic
        return listOf(
            Article("How Sunlight, the Immune System, and Covid-19 Interact", "For thousands of years, humans have recognized that the sun plays a role in the emergence and transmission of viruses"),
            Article("The Science Behind Improving Your Immune System", "Today i will talk about that science about your immune system that nobody ever talk about"),
            Article("Doctors Test the Limits of What Obesity Drugs Can Fix", "“Obesity first” doctors say they start with one medication, to treat obesity, and often find other chronic diseases."),
            Article("Researchers Say Social Media Warning Is Too Broad", "Some scientists who study youth mental health say the evidence does not support the notion that social media is harmful.")
        )
    }

    private fun signOut() {
        auth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this@HomeActivity, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
