package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktailapp.R

class TutorialDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tutorial_detail)

        val tutorialName = intent.getStringExtra("tutorialName")
        val tutorialDescription = intent.getStringExtra("tutorialDescription")

        val tvName = findViewById<TextView>(R.id.tvTutorialName)
        val tvDescription = findViewById<TextView>(R.id.tvTutorialDescription)

        tvName.text = tutorialName
        tvDescription.text = tutorialDescription
    }
}
