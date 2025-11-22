package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.models.Ingredient
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CocktailAdapter
    private lateinit var ingredientContainer: GridLayout

    private val firestore = FirebaseFirestore.getInstance()
    private var allCocktails = listOf<Cocktail>()
    private val selectedIngredients = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searsh)

        searchEditText = findViewById(R.id.searchEditText)
        recyclerView = findViewById(R.id.searchResultsRecyclerView)
        ingredientContainer = findViewById(R.id.ingredientContainer)

        adapter = CocktailAdapter(allCocktails)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchAllCocktails()

        // Apply filtering as the user types
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                applyFilters()
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun fetchAllCocktails() {
        firestore.collection("cocktails").get()
            .addOnSuccessListener { snapshot ->
                val allIngredients = mutableSetOf<String>()

                allCocktails = snapshot.map { doc ->
                    val ingredientList = doc.get("Ingredients") as? List<Map<String, String>>
                    val ingredients = ingredientList?.map { item ->
                        Ingredient(
                            name = item["Name"] ?: "",
                            quantity = item["Quantity"] ?: ""
                        )
                    } ?: listOf()

                    allIngredients.addAll(ingredients.map { it.name })

                    Cocktail(
                        name = doc.getString("name") ?: "",
                        flavourDescription = doc.getString("flavourDescription"),
                        history = doc.getString("history"),
                        expertRating = doc.getDouble("expertRating"),
                        id = doc.getDouble("id"),
                        ingredients = ingredients
                    )
                }

                Log.d("IngredientsFetched", "Ingredients: $allIngredients")

                setupIngredientButtons(allIngredients.toList().sorted())
                adapter.updateData(allCocktails)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load cocktails", Toast.LENGTH_SHORT).show()
                Log.e("Firestore", "Error fetching cocktails", it)
            }
    }

    private fun setupIngredientButtons(ingredients: List<String>) {
        ingredientContainer.removeAllViews()
        ingredientContainer.columnCount = 2

        ingredients.forEach { ingredient ->
            val checkBox = CheckBox(this).apply {
                text = ingredient
                setPadding(8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                }

                setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked) {
                        selectedIngredients.add(ingredient)
                    } else {
                        selectedIngredients.remove(ingredient)
                    }
                    applyFilters()
                }
            }
            ingredientContainer.addView(checkBox)
        }
    }

    private fun applyFilters() {
        val nameQuery = searchEditText.text.toString().trim()

        val filtered = allCocktails.filter { cocktail ->
            val matchesName = nameQuery.isEmpty() || cocktail.name?.contains(nameQuery, ignoreCase = true) == true
            val matchesIngredients = selectedIngredients.all { selected ->
                cocktail.ingredients.any { it.name == selected }
            }
            matchesName && matchesIngredients
        }

        adapter.updateData(filtered)
    }
}
