package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cocktailapp.R
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.databinding.ActivityFavoritesBinding
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
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
                    } else {
                        adapter.updateData(emptyList())
                        Toast.makeText(this, getString(R.string.info_aucun_favori), Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_chargement_favoris), Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchFavoriteCocktails(favoriteNames: List<String>) {
        val db = FirebaseFirestore.getInstance()
        val collected = mutableListOf<Cocktail>()
        val chunks = favoriteNames.chunked(10)
        if (chunks.isEmpty()) {
            adapter.updateData(emptyList())
            return
        }
        var remaining = chunks.size

        fun maybePublish() {
            remaining--
            if (remaining <= 0) {
                // on publie une seule fois la liste complète
                adapter.updateData(collected)
            }
        }

        chunks.forEach { chunk ->
            db.collection("cocktails")
                .whereIn(FieldPath.documentId(), chunk)
                .get()
                .addOnSuccessListener { documents ->
                    documents.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        fun str(key: String): String? = (data[key] as? String)?.takeIf { it.isNotBlank() } ?: data[key]?.toString()
                        fun num(key: String): Double? = when (val v = data[key]) {
                            is Number -> v.toDouble()
                            is String -> v.toDoubleOrNull()
                            else -> null
                        }
                        val ingList = mutableListOf<com.example.cocktailapp.models.Ingredient>()
                        (data["ingredients"] as? List<*>)?.forEach { item ->
                            if (item is Map<*, *>) {
                                val q = item["quantity"]?.toString()?.trim().orEmpty()
                                val n = item["name"]?.toString()?.trim().orEmpty()
                                ingList.add(com.example.cocktailapp.models.Ingredient(name = n, quantity = q))
                            }
                        }
                        collected.add(
                            Cocktail(
                                id = doc.id,
                                name = str("name"),
                                flavourDescription = str("flavourDescription"),
                                history = str("history"),
                                expertRating = num("expertRating"),
                                memberRating = num("memberRating"),
                                imageUrl = str("imageUrl"),
                                recipe = str("recipe"),
                                strengthScore = num("strengthScore"),
                                tasteScore = num("tasteScore"),
                                review = str("review"),
                                nutrition = str("nutrition"),
                                alcoholContent = str("alcoholContent"),
                                ingredients = ingList
                            )
                        )
                    }
                    maybePublish()
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.error_chargement_cocktails_favoris), Toast.LENGTH_SHORT).show()
                    maybePublish()
                }
        }
    }
}

