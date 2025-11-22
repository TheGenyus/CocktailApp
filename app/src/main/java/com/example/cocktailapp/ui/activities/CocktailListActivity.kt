package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailapp.R
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.databinding.ActivityCocktailListBinding
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.firestore.FirebaseFirestore

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
                documents.mapNotNullTo(cocktailList) {
                    it.toObject(Cocktail::class.java).takeUnless { cocktail -> cocktail.id.isBlank() }
                }
                cocktailAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, getString(R.string.error_loading_cocktails, exception.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }
}
