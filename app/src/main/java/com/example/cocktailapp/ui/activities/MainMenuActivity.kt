package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktailapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainMenuActivity : AppCompatActivity() {

    private lateinit var logoutBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)
        val firestore = FirebaseFirestore.getInstance()

        // Init buttons
        findViewById<Button>(R.id.btnFavorites).setOnClickListener {
            startActivity(Intent(this, FavoritesActivity::class.java))
        }

        findViewById<Button>(R.id.btnAll).setOnClickListener {
            startActivity(Intent(this, CocktailListActivity::class.java))
        }

        findViewById<Button>(R.id.btnSearch).setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }

        findViewById<Button>(R.id.btnRandom).setOnClickListener {
            startActivity(Intent(this, RandomActivity::class.java))
        }

        findViewById<Button>(R.id.btnTutorials).setOnClickListener {
            startActivity(Intent(this, TutorialsActivity::class.java))
        }

        logoutBtn = findViewById(R.id.btnLogout)
        logoutBtn.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
