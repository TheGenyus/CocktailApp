package com.example.cocktailapp.ui.activities

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.cocktailapp.R
import com.example.cocktailapp.databinding.ActivityCocktailDetailBinding
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.models.Ingredient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class CocktailDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCocktailDetailBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var partyCode: String? = null
    private val prefs: SharedPreferences by lazy { getSharedPreferences("party_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCocktailDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val cocktailId = intent.getStringExtra("cocktailId") ?: intent.getStringExtra("cocktail_id")
        partyCode = intent.getStringExtra("partyCode")
        if (cocktailId.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        loadCocktail(cocktailId)
    }

    private fun safeCocktailFromSnapshot(doc: com.google.firebase.firestore.DocumentSnapshot): Cocktail? {
        val data = doc.data ?: return null
        fun str(key: String): String? = (data[key] as? String)?.takeIf { it.isNotBlank() } ?: data[key]?.toString()
        fun num(key: String): Double? = when (val v = data[key]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
        val ingList = mutableListOf<Ingredient>()
        val rawIngs = data["ingredients"] as? List<*>
        rawIngs?.forEach { item ->
            if (item is Map<*, *>) {
                val q = item["quantity"]?.toString()?.trim().orEmpty()
                val n = item["name"]?.toString()?.trim().orEmpty()
                ingList.add(Ingredient(name = n, quantity = q))
            }
        }
        return Cocktail(
            id = doc.id,
            name = str("name"),
            flavourDescription = str("flavourDescription"),
            history = str("history"),
            expertRating = num("expertRating"),
            memberRating = num("memberRating"),
            imageUrl = str("imageUrl") ?: str("image"),
            recipe = str("recipe") ?: str("instructions"),
            strengthScore = num("strengthScore") ?: (data["profile"] as? Map<*, *>)?.let { numFromMap(it, "strength") },
            tasteScore = num("tasteScore") ?: (data["profile"] as? Map<*, *>)?.let { numFromMap(it, "sweetness") },
            review = str("review"),
            nutrition = str("nutrition"),
            alcoholContent = str("alcoholContent"),
            ingredients = ingList
        )
    }

    private fun numFromMap(map: Map<*, *>, key: String): Double? {
        return when (val v = map[key]) {
            is Number -> v.toDouble()
            is String -> v.toDoubleOrNull()
            else -> null
        }
    }

    private fun loadCocktail(cocktailId: String) {
        firestore.collection("cocktails")
            .document(cocktailId)
            .get()
            .addOnSuccessListener { document ->
                val cocktail = safeCocktailFromSnapshot(document)
                if (cocktail == null) {
                    Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                val imageFromDocument = document.getString("image")
                renderCocktail(cocktailId, cocktail, imageFromDocument)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_cocktail_inconnu), Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun renderCocktail(cocktailId: String, cocktail: Cocktail, imageFallback: String?) {
        val name = cocktail.name ?: getString(R.string.label_nom_inconnu)
        binding.tvName.text = name

        val imageUrl = cocktail.imageUrl?.takeIf { it.isNotBlank() } ?: imageFallback
        if (!imageUrl.isNullOrBlank()) {
            binding.ivImage.visibility = View.VISIBLE
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_launcher_foreground)
                .fitCenter()
                .into(binding.ivImage)
        } else {
            binding.ivImage.visibility = View.GONE
        }

        setSection(binding.tvTitleRecipe, binding.tvRecipe, cocktail.recipe)

        if (cocktail.ingredients.isNotEmpty()) {
            val ingText = cocktail.ingredients.joinToString(separator = "\n") {
                val q = it.quantity.takeIf { q -> !q.isNullOrBlank() } ?: ""
                val n = it.name.takeIf { n -> !n.isNullOrBlank() } ?: ""
                listOf(q, n).filter { part -> part.isNotBlank() }.joinToString(" ").trim()
            }
            if (ingText.isNotBlank()) {
                binding.tvTitleIngredients.visibility = View.VISIBLE
                binding.tvIngredients.visibility = View.VISIBLE
                binding.tvIngredients.text = ingText
            } else {
                binding.tvTitleIngredients.visibility = View.GONE
                binding.tvIngredients.visibility = View.GONE
            }
        } else {
            binding.tvTitleIngredients.visibility = View.GONE
            binding.tvIngredients.visibility = View.GONE
        }

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

        setSection(binding.tvTitleHistory, binding.tvHistory, cocktail.history)
        setSection(binding.tvTitleReview, binding.tvReview, cocktail.review)
        setSection(binding.tvTitleNutrition, binding.tvNutrition, cocktail.nutrition)
        setSection(binding.tvTitleAlcohol, binding.tvAlcohol, cocktail.alcoholContent)
        setSection(binding.tvTitleGout, binding.tvGout, cocktail.flavourDescription)

        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            binding.btnFavorite.visibility = View.GONE
        } else {
            binding.btnFavorite.visibility = View.VISIBLE
            firestore.collection("users").document(currentUserId)
                .get()
                .addOnSuccessListener { doc ->
                    val favorites = doc.get("favorites") as? List<*> ?: emptyList<Any>()
                    val isFav = favorites.any { it?.toString() == cocktailId }
                    updateFavoriteButton(cocktailId, name, currentUserId, isFav)
                }
                .addOnFailureListener {
                    updateFavoriteButton(cocktailId, name, currentUserId, false)
                }
        }

        val userId = auth.currentUser?.uid
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

        val party = partyCode
        if (party.isNullOrBlank()) {
            binding.btnPartyOrder.visibility = View.GONE
        } else {
            binding.btnPartyOrder.visibility = View.VISIBLE
            binding.btnPartyOrder.setOnClickListener {
                val currentUser = auth.currentUser?.uid
                if (currentUser == null) {
                    Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val displayName = prefs.getString("display_name", "")?.trim().orEmpty()
                val data = hashMapOf(
                    "userId" to currentUser,
                    "userName" to displayName,
                    "cocktailId" to cocktailId,
                    "cocktailName" to name,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                firestore.collection("parties").document(party)
                    .collection("orders")
                    .add(data)
                    .addOnSuccessListener {
                        Toast.makeText(this, getString(R.string.party_commander), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, getString(R.string.party_error_commande) + " : " + (e.message ?: ""), Toast.LENGTH_SHORT).show()
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
    private fun updateFavoriteButton(cocktailId: String, name: String, userId: String, isFav: Boolean) {
        if (isFinishing || isDestroyed) return
        binding.btnFavorite.text = if (isFav) getString(R.string.action_remove_favorite) else getString(R.string.action_add_favorite)
        binding.btnFavorite.setOnClickListener {
            val op = if (isFav) FieldValue.arrayRemove(cocktailId) else FieldValue.arrayUnion(cocktailId)
            firestore.collection("users").document(userId)
                .update("favorites", op)
                .addOnSuccessListener {
                    val newState = !isFav
                    updateFavoriteButton(cocktailId, name, userId, newState)
                    val msg = if (newState) getString(R.string.info_favori_ajoute, name) else getString(R.string.info_favori_retrait, name)
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, getString(R.string.error_favori, e.message ?: ""), Toast.LENGTH_SHORT).show()
                }
        }
    }
}
