package com.capstoneproject.beautyboost.ui

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.RadioButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.capstoneproject.beautyboost.R
import com.capstoneproject.beautyboost.databinding.ActivityMainBinding
import com.capstoneproject.beautyboost.databinding.ActivityProfileBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private var skinType: String = ""
    private var gender: String = ""

    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        enableEdgeToEdge()

        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfile)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val firebaseUser = auth.currentUser

        if (firebaseUser == null) {
            // Not signed in, launch the Login activity
            startActivity(Intent(this, SignInActivity::class.java))
            finish()
            return
        }else{
            val userName = firebaseUser.displayName?.split(" ")?.get(0) ?: "User"
            binding.nameTextField.editText?.setText(userName)
        }

        val btnLogout = findViewById<Button>(R.id.logoutButton)
        btnLogout.setOnClickListener{
            signOut()
        }

        binding.skinTypeRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButton = findViewById<RadioButton>(checkedId)
            skinType = radioButton.text.toString()
        }

        // Set up the gender radio group
        binding.genderRadioGroup.setOnCheckedChangeListener { group, checkedId ->
            val radioButton = findViewById<RadioButton>(checkedId)
            gender = radioButton.text.toString()
        }

        binding.submitButton.setOnClickListener {

            val name = binding.nameTextField.editText?.text.toString()
            val age = binding.ageEditText.text.toString()

            if (name.isNotEmpty() && age.isNotEmpty() && skinType.isNotEmpty() && gender.isNotEmpty()) {
                val message = "Name: $name, Age: $age, Skin Type: $skinType, Gender: $gender"
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                val moveIntent = Intent(this@ProfileActivity, HomeActivity::class.java)
                startActivity(moveIntent)
            } else {
                Toast.makeText(this, "Please fill all the fields", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun signOut() {
        auth.signOut()

        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            val intent = Intent(this@ProfileActivity, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
