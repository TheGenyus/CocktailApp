package com.example.cocktailapp.models

data class Party(
    var code: String = "",
    var ownerId: String = "",
    var barmen: List<String> = emptyList(),
    var active: Boolean = true,
    var maxMissingIngredients: Int = 0,
    var cocktailIds: List<String> = emptyList(),
    var createdAt: Long = 0L
)
