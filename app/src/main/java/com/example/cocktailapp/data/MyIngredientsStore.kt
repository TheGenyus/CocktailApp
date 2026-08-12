package com.example.cocktailapp.data

import android.content.Context

object MyIngredientsStore {
    private const val PREFS_NAME = "my_ingredients"
    private const val KEY_INGREDIENTS = "ingredients"

    fun load(context: Context): Set<String> {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_INGREDIENTS, emptySet())
            ?.toSet()
            ?: emptySet()
    }

    fun save(context: Context, ingredients: Set<String>) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_INGREDIENTS, ingredients.toSet())
            .apply()
    }
}
