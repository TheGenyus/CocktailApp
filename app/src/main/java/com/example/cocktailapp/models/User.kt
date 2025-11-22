package com.example.cocktailapp.models

data class User(
    val uid: String = "",
    val favorites: List<String> = listOf(),
    val history: List<String> = listOf(),
    val ingredients: List<String> = listOf(),
    val preferredFlavours: List<String> = listOf()
)
