package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.adapters.CocktailAdapter
import com.google.firebase.firestore.FirebaseFirestore
import com.example.cocktailapp.databinding.ActivityCocktailListBinding // Import the binding class

class CocktailListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCocktailListBinding // Declare the binding object
    private val cocktailList = mutableListOf<Cocktail>()
    private lateinit var cocktailAdapter: CocktailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the binding object
        binding = ActivityCocktailListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up RecyclerView using the binding object
        cocktailAdapter = CocktailAdapter(cocktailList)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = cocktailAdapter

        // Load cocktails from Firestore
        loadCocktails()
    }

    private fun loadCocktails() {
        val db = FirebaseFirestore.getInstance()
        db.collection("cocktails")
            .get()
            .addOnSuccessListener { documents ->
                cocktailList.clear()
                for (document in documents) {
                    val cocktail = document.toObject(Cocktail::class.java)
                    cocktailList.add(cocktail)
                }
                cocktailAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error loading cocktails: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
