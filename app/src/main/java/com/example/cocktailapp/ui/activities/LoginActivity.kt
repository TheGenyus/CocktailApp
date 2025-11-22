package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
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
            val currentUser = auth.currentUser
            auth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                val userDocRef = currentUser?.let { it1 -> db.collection("users").document(it1.uid) }
                // Check if the user already exists
                if (userDocRef != null) {
                    userDocRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            // Create a new user document
                            val newUser = hashMapOf(
                                "userId" to currentUser.uid,
                                "favorites" to listOf<String>() // or don't include it if you're using a subcollection
                            )
                            userDocRef.set(newUser)
                        }
                    }
                }
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Login failed!", Toast.LENGTH_SHORT).show()
            }
        }

        registerBtn.setOnClickListener {
            val email = emailField.text.toString()
            val password = passwordField.text.toString()
            auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                val currentUser = auth.currentUser
                val userDocRef = currentUser?.let { it1 -> db.collection("users").document(it1.uid) }
                // Check if the user already exists
                if (userDocRef != null) {
                    userDocRef.get().addOnSuccessListener { document ->
                        if (!document.exists()) {
                            // Create a new user document
                            val newUser = hashMapOf(
                                "userId" to currentUser.uid,
                                "favorites" to listOf<String>() // or don't include it if you're using a subcollection
                            )
                            userDocRef.set(newUser)
                        }
                    }
                }
                startActivity(Intent(this, MainMenuActivity::class.java))
                finish()
            }.addOnFailureListener {
                Toast.makeText(this, "Registration failed!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
