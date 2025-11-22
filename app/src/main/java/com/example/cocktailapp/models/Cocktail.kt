package com.example.cocktailapp.models

import com.google.firebase.firestore.DocumentId

data class Cocktail(
    @DocumentId val id: String = "",
    val name: String? = null,
    val flavourDescription: String? = null,
    val history: String? = null,
    val expertRating: Double? = null,
    val ingredients: List<Ingredient> = emptyList()
)
