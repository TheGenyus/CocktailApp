package com.example.cocktailapp.models

data class PartyOrder(
    var id: String = "",
    var userId: String = "",
    var cocktailId: String = "",
    var cocktailName: String = "",
    var userName: String = "",
    var status: String = "pending",
    var createdAt: Long = 0L
)

