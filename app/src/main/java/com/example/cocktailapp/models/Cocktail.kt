package com.example.cocktailapp.models

import com.google.firebase.firestore.DocumentId

data class Cocktail(
    @DocumentId val id: String = "",
    val name: String? = null,
    val flavourDescription: String? = null,
    val history: String? = null,
    val expertRating: Double? = null,
    val memberRating: Double? = null,
    val imageUrl: String? = null,
    val recipe: String? = null,
    val strengthScore: Double? = null,
    val tasteScore: Double? = null,
    val review: String? = null,
    val nutrition: String? = null,
    val alcoholContent: String? = null,
    val ingredients: List<Ingredient> = emptyList()
)