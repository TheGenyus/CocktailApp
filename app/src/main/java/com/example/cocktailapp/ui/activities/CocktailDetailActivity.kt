package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktailapp.R
import com.example.cocktailapp.databinding.ActivityCocktailDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CocktailDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCocktailDetailBinding
    private lateinit var ratingBar: RatingBar
    private lateinit var saveRatingButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCocktailDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra("cocktailName") ?: return

        // Set static data
        binding.tvName.text = name
        binding.tvGout.text = intent.getStringExtra("cocktailGout")
        binding.tvExpertRating.text = "Note des experts : ${intent.getDoubleExtra("cocktailExpertRating", 0.0)}"
        binding.tvHistory.text = intent.getStringExtra("cocktailHistory")

        // Fetch full cocktail doc for ingredients
        firestore.collection("cocktails")
            .whereEqualTo("name", name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val document = querySnapshot.documents.firstOrNull()
                if (document != null && document.exists()) {
                    val ingredientsList = document.get("Ingredients") as? List<Map<String, Any>>
                    val ingredientsText =
                        ("Ingrédients :\n" + ingredientsList?.joinToString("\n") {
                            val ingredientName = it["Name"] as? String ?: "Ingrédient inconnu"
                            val quantity = it["Quantity"] as? String ?: "Quantité inconnue"
                            "- $ingredientName : $quantity"
                        }) ?: "Aucun ingrédient trouvé."
                    binding.tvIngredients.text = ingredientsText
                }
            }

        // Favorites logic
        val btnFavorite = findViewById<Button>(R.id.btnFavorite)
        val btnRemove = findViewById<Button>(R.id.btnRemoveFromFavorites)

        btnFavorite.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(currentUserId)
                .update("favorites", FieldValue.arrayUnion(name))
                .addOnSuccessListener {
                    Toast.makeText(this, "$name added to your favorites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnRemove.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(currentUserId)
                .update("favorites", FieldValue.arrayRemove(name))
                .addOnSuccessListener {
                    Toast.makeText(this, "$name removed from favorites!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to remove favorite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // ⭐️ Rating Feature
        ratingBar = binding.userRatingBar
        saveRatingButton = binding.saveRatingButton

        val userId = auth.currentUser?.uid ?: return
        val ratingDocId = "$userId-$name"

        // Load existing rating
        firestore.collection("ratings").document(ratingDocId)
            .get()
            .addOnSuccessListener { document ->
                val savedRating = document.getDouble("rating") ?: 0.0
                ratingBar.rating = savedRating.toFloat()
            }

        // Save/update rating
        saveRatingButton.setOnClickListener {
            val rating = ratingBar.rating
            val ratingData = mapOf(
                "userId" to userId,
                "cocktailName" to name,
                "rating" to rating
            )

            firestore.collection("ratings").document(ratingDocId)
                .set(ratingData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Rating saved!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save rating.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
