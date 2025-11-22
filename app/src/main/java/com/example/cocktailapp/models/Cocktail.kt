package com.example.cocktailapp.models

data class Cocktail(
    var name: String? = null,
    var flavourDescription: String? = null,
    var history: String? = null,
    var expertRating: Double? = null,
    var id: Double? = null,
    val ingredients: List<Ingredient> = listOf()
) {
    // No-arg constructor required by Firebase
    constructor() : this(null, null, null, null, null)
}
