package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktailapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val emailField = findViewById<EditText>(R.id.emailEditText)
        val passwordField = findViewById<EditText>(R.id.passwordEditText)
        val loginBtn = findViewById<Button>(R.id.loginButton)
        val registerBtn = findViewById<Button>(R.id.registerButton)
        val db = FirebaseFirestore.getInstance()

        loginBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (!isValidCredentials(email, password)) return@setOnClickListener

            setButtonsEnabled(loginBtn, registerBtn, false)
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                val currentUser = auth.currentUser ?: return@addOnSuccessListener
                val userDocRef = db.collection("users").document(currentUser.uid)
                userDocRef.get().addOnSuccessListener { document ->
                    if (!document.exists()) {
                        val newUser = hashMapOf(
                            "userId" to currentUser.uid,
                            "favorites" to listOf<String>()
                        )
                        userDocRef.set(newUser)
                    }
                }
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                setButtonsEnabled(loginBtn, registerBtn, true)
            }
        }

        registerBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()

            if (!isValidCredentials(email, password)) return@setOnClickListener

            setButtonsEnabled(loginBtn, registerBtn, false)
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                val currentUser = auth.currentUser ?: return@addOnSuccessListener
                val userDocRef = db.collection("users").document(currentUser.uid)
                userDocRef.get().addOnSuccessListener { document ->
                    if (!document.exists()) {
                        val newUser = hashMapOf(
                            "userId" to currentUser.uid,
                            "favorites" to listOf<String>()
                        )
                        userDocRef.set(newUser)
                    }
                }
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_registration_failed), Toast.LENGTH_SHORT).show()
            }.addOnCompleteListener {
                setButtonsEnabled(loginBtn, registerBtn, true)
            }
        }
    }

    private fun isValidCredentials(email: String, password: String): Boolean {
        if (email.isBlank()) {
            Toast.makeText(this, getString(R.string.error_email_required), Toast.LENGTH_SHORT).show()
            return false
        }
        if (password.length < 8) {
            Toast.makeText(this, getString(R.string.error_password_length), Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setButtonsEnabled(loginBtn: Button, registerBtn: Button, enabled: Boolean) {
        loginBtn.isEnabled = enabled
        registerBtn.isEnabled = enabled
    }
}
