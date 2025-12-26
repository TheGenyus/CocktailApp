package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cocktailapp.R
import com.example.cocktailapp.databinding.ActivityCocktailDetailBinding
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CocktailDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCocktailDetailBinding
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

        loadCocktail(cocktailId)
    }

    private fun loadCocktail(cocktailId: String) {
        firestore.collection("cocktails")
            .document(cocktailId)
            .get()
            .addOnSuccessListener { document ->
                val cocktail = document.toObject(Cocktail::class.java)
                if (cocktail == null) {
                    Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                renderCocktail(cocktailId, cocktail)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun renderCocktail(cocktailId: String, cocktail: Cocktail) {
        val name = cocktail.name ?: getString(R.string.label_nom_inconnu)
        binding.tvName.text = name

        // Image
        if (!cocktail.imageUrl.isNullOrBlank()) {
            binding.ivImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(cocktail.imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .into(binding.ivImage)
        } else {
            binding.ivImage.visibility = View.GONE
        }

        // Recipe
        setSection(binding.tvTitleRecipe, binding.tvRecipe, cocktail.recipe)

        // Ingredients
        if (cocktail.ingredients.isNotEmpty()) {
            val ingText = cocktail.ingredients.joinToString(separator = "\n") {
                "- ${it.quantity} ${it.name}"
            }
            binding.tvTitleIngredients.visibility = View.VISIBLE
            binding.tvIngredients.visibility = View.VISIBLE
            binding.tvIngredients.text = ingText
        } else {
            binding.tvTitleIngredients.visibility = View.GONE
            binding.tvIngredients.visibility = View.GONE
        }

        // Strength / taste scores
        val strengthScore = cocktail.strengthScore
        val tasteScore = cocktail.tasteScore
        val strengthLines = mutableListOf<String>()
        strengthScore?.let { strengthLines.add("Force: ${it}/10") }
        tasteScore?.let { strengthLines.add("Douceur/acidite: ${it}/10") }
        if (strengthLines.isNotEmpty()) {
            binding.tvTitleStrength.visibility = View.VISIBLE
            binding.tvStrengthScores.visibility = View.VISIBLE
            binding.tvStrengthScores.text = strengthLines.joinToString(" | ")
        } else {
            binding.tvTitleStrength.visibility = View.GONE
            binding.tvStrengthScores.visibility = View.GONE
        }

        // Ratings
        val ratingLines = mutableListOf<String>()
        cocktail.expertRating?.let { ratingLines.add("Note expert: ${it}/5") }
        cocktail.memberRating?.let { ratingLines.add("Note membres: ${it}/5") }
        if (ratingLines.isNotEmpty()) {
            binding.tvTitleRatings.visibility = View.VISIBLE
            binding.tvRatings.visibility = View.VISIBLE
            binding.tvRatings.text = ratingLines.joinToString("\n")
        } else {
            binding.tvTitleRatings.visibility = View.GONE
            binding.tvRatings.visibility = View.GONE
        }

        // History, Review, Nutrition, Alcohol, Description
        setSection(binding.tvTitleHistory, binding.tvHistory, cocktail.history)
        setSection(binding.tvTitleReview, binding.tvReview, cocktail.review)
        setSection(binding.tvTitleNutrition, binding.tvNutrition, cocktail.nutrition)
        setSection(binding.tvTitleAlcohol, binding.tvAlcohol, cocktail.alcoholContent)
        setSection(binding.tvTitleGout, binding.tvGout, cocktail.flavourDescription)

        // Favorites toggle
        val userId = auth.currentUser?.uid
        if (userId == null) {
            binding.btnFavorite.visibility = View.GONE
        } else {
            binding.btnFavorite.visibility = View.VISIBLE
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val favorites = doc.get("favorites") as? List<*> ?: emptyList<Any>()
                    var isFav = favorites.contains(cocktailId)
                    updateFavoriteButton(isFav)
                    binding.btnFavorite.setOnClickListener {
                        val targetState = !isFav
                        val updateOp = if (targetState) FieldValue.arrayUnion(cocktailId) else FieldValue.arrayRemove(cocktailId)
                        firestore.collection("users").document(userId)
                            .update("favorites", updateOp)
                            .addOnSuccessListener {
                                isFav = targetState
                                updateFavoriteButton(isFav)
                                val msg = if (isFav) {
                                    getString(R.string.info_favori_ajoute, name)
                                } else {
                                    getString(R.string.info_favori_retrait, name)
                                }
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, getString(R.string.error_favori, e.message ?: ""), Toast.LENGTH_SHORT).show()
                            }
                    }
                }
        }

        // User rating
        if (userId == null) {
            binding.userRatingBar.visibility = View.GONE
            binding.saveRatingButton.visibility = View.GONE
        } else {
            binding.userRatingBar.visibility = View.VISIBLE
            binding.saveRatingButton.visibility = View.VISIBLE
            val ratingDocId = "$userId-$cocktailId"
            firestore.collection("ratings").document(ratingDocId)
                .get()
                .addOnSuccessListener { document ->
                    val savedRating = document.getDouble("rating") ?: 0.0
                    binding.userRatingBar.rating = savedRating.toFloat()
                }
            binding.saveRatingButton.setOnClickListener {
                val rating = binding.userRatingBar.rating
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

    private fun setSection(title: View, contentView: View, text: String?) {
        if (contentView is android.widget.TextView && title is android.widget.TextView) {
            if (!text.isNullOrBlank()) {
                title.visibility = View.VISIBLE
                contentView.visibility = View.VISIBLE
                contentView.text = text
            } else {
                title.visibility = View.GONE
                contentView.visibility = View.GONE
            }
        } else if (contentView is android.widget.TextView) {
            if (!text.isNullOrBlank()) {
                contentView.visibility = View.VISIBLE
                contentView.text = text
            } else {
                contentView.visibility = View.GONE
            }
        }
    }

    private fun updateFavoriteButton(isFavorite: Boolean) {
        binding.btnFavorite.text = if (isFavorite) "Retirer des favoris" else "Ajouter aux favoris"
    }
}
