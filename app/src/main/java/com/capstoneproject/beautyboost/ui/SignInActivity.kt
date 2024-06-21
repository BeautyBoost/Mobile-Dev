package com.capstoneproject.beautyboost.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.capstoneproject.beautyboost.MainActivity
import com.capstoneproject.beautyboost.R
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class SignInActivity : AppCompatActivity() {

    companion object {
        private const val RC_SIGN_IN = 9001
    }

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signin)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // The user is already signed in, navigate to MainActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        val googleButton = findViewById<ConstraintLayout>(R.id.BtnGoogle)
        googleButton.setOnClickListener {
            showLoadingDialog()
            signIn()
        }

        val guestButton = findViewById<Button>(R.id.BtnGuest)
        guestButton.setOnClickListener {
            showLoadingDialog()
            signInGuest()
        }
    }

    private fun signIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        val googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun signInGuest() {
        auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    showSuccessDialog(user?.displayName)
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    showSuccessDialog(user?.displayName)
                } else {
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showSuccessDialog(userName: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_success, null)
        val checkIcon = dialogView.findViewById<ImageView>(R.id.checkIcon)
        val successText = dialogView.findViewById<TextView>(R.id.successText)
        val userNameText = dialogView.findViewById<TextView>(R.id.userNameText)
        val okButton = dialogView.findViewById<Button>(R.id.okButton)

        userNameText.text = "Welcome, ${userName ?: "User"}!"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        okButton.setOnClickListener {
            dialog.dismiss()
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }

        dialog.show()
    }

    private fun showLoadingDialog() {
        val loaderView = layoutInflater.inflate(R.layout.custom_loading, null)
        val loaderDialog = AlertDialog.Builder(this)
            .setView(loaderView)
            .setCancelable(false)
            .create()
        loaderDialog.show()
    }
}
