package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailapp.adapters.TutorialAdapter
import com.example.cocktailapp.databinding.ActivityTutorialsBinding
import com.example.cocktailapp.models.Tutorial
import com.google.firebase.firestore.FirebaseFirestore

class TutorialsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTutorialsBinding
    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: TutorialAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTutorialsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TutorialAdapter(emptyList()) // Start with empty list
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        loadTutorials()
    }

    private fun loadTutorials() {
        db.collection("Tutorials")
            .get()
            .addOnSuccessListener { result ->
                val tutorials = result.documents.mapNotNull { doc ->
                    val name = doc.id
                    val description = doc.getString("Tuto") ?: return@mapNotNull null
                    Tutorial(name, description)
                }
                adapter.updateData(tutorials)
            }
            .addOnFailureListener {
                // Handle error if needed (toast, log, etc.)
            }
    }
}
