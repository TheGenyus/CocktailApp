package com.example.cocktailapp.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.firestore.FirebaseFirestore
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors
import org.json.JSONArray
import org.json.JSONObject

class CocktailRepository(private val context: Context) {

    private val firestore = FirebaseFirestore.getInstance()
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile private var cache: List<Cocktail> = emptyList()

    fun fetchCocktails(onSuccess: (List<Cocktail>) -> Unit, onError: (Exception) -> Unit) {
        if (cache.isNotEmpty()) {
            onSuccess(cache)
            return
        }
        val url = context.getString(R.string.cocktails_json_url)
        if (url.isNotBlank() && url.startsWith("http")) {
            fetchFromJson(url, onSuccess, onError)
        } else {
            fetchFromFirestore(onSuccess, onError)
        }
    }

    private fun fetchFromJson(urlStr: String, onSuccess: (List<Cocktail>) -> Unit, onError: (Exception) -> Unit) {
        executor.execute {
            try {
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                conn.connect()
                if (conn.responseCode !in 200..299) throw Exception("HTTP ${conn.responseCode}")
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val content = reader.use { it.readText() }
                val parsed = parseCocktailsJson(content)
                cache = parsed
                mainHandler.post { onSuccess(parsed) }
            } catch (e: Exception) {
                mainHandler.post { fetchFromFirestore(onSuccess, onError) }
            }
        }
    }

    private fun parseCocktailsJson(content: String): List<Cocktail> {
        val arr = JSONArray(content)
        val list = mutableListOf<Cocktail>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(parseCocktail(obj))
        }
        return list
    }

    private fun parseCocktail(obj: JSONObject): Cocktail {
        val ingredientsJson = obj.optJSONArray("ingredients") ?: JSONArray()
        val ingredients = mutableListOf<com.example.cocktailapp.models.Ingredient>()
        for (j in 0 until ingredientsJson.length()) {
            val ing = ingredientsJson.getJSONObject(j)
            ingredients.add(
                com.example.cocktailapp.models.Ingredient(
                    quantity = ing.optString("quantity"),
                    name = ing.optString("name")
                )
            )
        }

        val profile = obj.optJSONObject("profile")
        val strengthFromProfile = profile?.optDouble("strength", Double.NaN)
        val tasteFromProfile = profile?.optDouble("sweetness", Double.NaN)
        val strengthScore = when {
            strengthFromProfile != null && !strengthFromProfile.isNaN() -> strengthFromProfile
            else -> obj.optDouble("strengthScore", Double.NaN)
        }.let { if (it.isNaN()) null else it }
        val tasteScore = when {
            tasteFromProfile != null && !tasteFromProfile.isNaN() -> tasteFromProfile
            else -> obj.optDouble("tasteScore", Double.NaN)
        }.let { if (it.isNaN()) null else it }

        return Cocktail(
            id = obj.optString("id"),
            name = obj.optString("name"),
            flavourDescription = obj.optString("flavourDescription"),
            history = obj.optString("history"),
            expertRating = obj.optDouble("expertRating", Double.NaN).let { if (it.isNaN()) null else it },
            memberRating = obj.optDouble("memberRating", Double.NaN).let { if (it.isNaN()) null else it },
            imageUrl = obj.optString("imageUrl", obj.optString("image")),
            recipe = obj.optString("recipe", obj.optString("instructions")),
            strengthScore = strengthScore,
            tasteScore = tasteScore,
            review = obj.optString("review"),
            nutrition = obj.optString("nutrition"),
            alcoholContent = obj.optString("alcoholContent"),
            ingredients = ingredients
        )
    }

    private fun fetchFromFirestore(onSuccess: (List<Cocktail>) -> Unit, onError: (Exception) -> Unit) {
        firestore.collection("cocktails").get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.mapNotNull { doc ->
                    val cocktail = doc.toObject(Cocktail::class.java)
                    if (cocktail.id.isBlank()) cocktail.copy(id = doc.id) else cocktail
                }
                cache = result
                onSuccess(result)
            }
            .addOnFailureListener { e -> onError(e) }
    }
}
