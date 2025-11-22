package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.databinding.ActivityFavoritesBinding
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FavoritesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFavoritesBinding
    private lateinit var adapter: CocktailAdapter
    private val cocktailList = mutableListOf<Cocktail>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CocktailAdapter(cocktailList)
        binding.recyclerViewFavorites.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFavorites.adapter = adapter

        loadFavorites()
    }

    private fun loadFavorites() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val userDocRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(currentUserId)

        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val favorites = document.get("favorites") as? List<String> ?: emptyList()
                    if (favorites.isNotEmpty()) {
                        fetchFavoriteCocktails(favorites)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load favorites", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFavoriteCocktails(favoriteNames: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val cocktailList = mutableListOf<Cocktail>()  // your Cocktail model class

        // Fetch all cocktails in a single query (if names are unique)
        db.collection("cocktails")
            .whereIn(
                "name",
                favoriteNames.take(10)
            ) // Firestore has a limit of 10 items in `whereIn`
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val cocktail = doc.toObject(Cocktail::class.java)
                    cocktailList.add(cocktail)
                }

                // Bind to adapter
                binding.recyclerViewFavorites.adapter = CocktailAdapter(cocktailList)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to fetch cocktail details", Toast.LENGTH_SHORT).show()
            }
    }
}

