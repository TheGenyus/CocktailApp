package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cocktailapp.R
import com.example.cocktailapp.databinding.ActivityCocktailDetailBinding
import com.example.cocktailapp.models.Cocktail
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

        val cocktailId = intent.getStringExtra("cocktailId")
        if (cocktailId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val name = intent.getStringExtra("cocktailName") ?: getString(R.string.label_nom_inconnu)
        val flavourDescription = intent.getStringExtra("cocktailGout") ?: ""
        val history = intent.getStringExtra("cocktailHistory") ?: ""
        val expertRating = intent.getDoubleExtra("cocktailExpertRating", 0.0)
        val ingredientLines = intent.getStringArrayListExtra("cocktailIngredients") ?: arrayListOf()

        binding.tvName.text = name
        binding.tvGout.text = flavourDescription.ifBlank { getString(R.string.label_info_non_disponible) }
        binding.tvExpertRating.text = getString(R.string.label_note_experts, expertRating)
        binding.tvHistory.text = history.ifBlank { getString(R.string.label_info_non_disponible) }
        binding.tvIngredients.text = if (ingredientLines.isNotEmpty()) {
            getString(R.string.label_ingredients_prefix, ingredientLines.joinToString("\n") { "- $it" })
        } else {
            getString(R.string.label_ingredients_absents)
        }

        firestore.collection("cocktails")
            .document(cocktailId)
            .get()
            .addOnSuccessListener { document ->
                val refreshed = document.toObject(Cocktail::class.java)
                refreshed?.let { cocktail ->
                    binding.tvName.text = cocktail.name ?: name
                    binding.tvGout.text = cocktail.flavourDescription?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.label_info_non_disponible)
                    binding.tvHistory.text = cocktail.history?.takeIf { it.isNotBlank() }
                        ?: getString(R.string.label_info_non_disponible)
                    binding.tvExpertRating.text = getString(
                        R.string.label_note_experts,
                        cocktail.expertRating ?: 0.0
                    )
                    if (cocktail.ingredients.isNotEmpty()) {
                        val refreshedLines = cocktail.ingredients.joinToString("\n") {
                            "- ${it.quantity} ${it.name}"
                        }
                        binding.tvIngredients.text = getString(R.string.label_ingredients_prefix, refreshedLines)
                    }
                }
            }

        val btnFavorite = findViewById<Button>(R.id.btnFavorite)
        val btnRemove = findViewById<Button>(R.id.btnRemoveFromFavorites)

        btnFavorite.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(currentUserId)
                .update("favorites", FieldValue.arrayUnion(cocktailId))
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.info_favori_ajoute, name), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.error_favori, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
        }

        btnRemove.setOnClickListener {
            val currentUserId = auth.currentUser?.uid ?: return@setOnClickListener
            firestore.collection("users").document(currentUserId)
                .update("favorites", FieldValue.arrayRemove(cocktailId))
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.info_favori_retrait, name), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.error_favori, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
        }

        ratingBar = binding.userRatingBar
        saveRatingButton = binding.saveRatingButton

        val userId = auth.currentUser?.uid ?: return
        val ratingDocId = "$userId-$cocktailId"

        firestore.collection("ratings").document(ratingDocId)
            .get()
            .addOnSuccessListener { document ->
                val savedRating = document.getDouble("rating") ?: 0.0
                ratingBar.rating = savedRating.toFloat()
            }

        saveRatingButton.setOnClickListener {
            val rating = ratingBar.rating
            val ratingData = mapOf(
                "userId" to userId,
                "cocktailId" to cocktailId,
                "cocktailName" to name,
                "rating" to rating
            )

            firestore.collection("ratings").document(ratingDocId)
                .set(ratingData)
                .addOnSuccessListener {
                    Toast.makeText(this, getString(R.string.info_note_enregistree), Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, getString(R.string.error_note), Toast.LENGTH_SHORT).show()
                }
        }
    }
}
