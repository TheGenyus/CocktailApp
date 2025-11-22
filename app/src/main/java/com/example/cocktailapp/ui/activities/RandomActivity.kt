package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RandomActivity : AppCompatActivity() {

    private lateinit var ingredientContainer: GridLayout
    private lateinit var selectRandomButton: Button
    private val selectedIngredients = mutableSetOf<String>()
    private val allCocktails = mutableListOf<Cocktail>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_cocktail_by_ingredient)

        ingredientContainer = findViewById(R.id.randomIngredientContainer)
        selectRandomButton = findViewById(R.id.btnSelectRandom)

        fetchCocktails()

        selectRandomButton.setOnClickListener {
            selectRandomCocktail()
        }

        // Add this to ensure content is below system UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.contentLayout)) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = systemInsets.top)
            insets
        }
    }

    private fun fetchCocktails() {
        FirebaseFirestore.getInstance().collection("cocktails")
            .get()
            .addOnSuccessListener { snapshot ->
                val allIngredients = mutableSetOf<String>()

                snapshot.documents.forEach { doc ->
                    val cocktail = doc.toObject(Cocktail::class.java)
                    if (cocktail != null && cocktail.id.isNotBlank()) {
                        allIngredients.addAll(cocktail.ingredients.map { it.name })
                        allCocktails.add(cocktail)
                    }
                }

                setupIngredientCheckboxes(allIngredients.filter { it.isNotBlank() }.toList().sorted())
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupIngredientCheckboxes(ingredients: List<String>) {
        ingredientContainer.columnCount = 2
        ingredientContainer.removeAllViews()

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
                }
            }
            ingredientContainer.addView(checkBox)
        }
    }

    private fun selectRandomCocktail() {
        if (selectedIngredients.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_selection_ingredients), Toast.LENGTH_SHORT).show()
            return
        }

        val matchingCocktails = allCocktails.filter { cocktail ->
            cocktail.ingredients.any { selectedIngredients.contains(it.name) }
        }

        if (matchingCocktails.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_aucun_cocktail), Toast.LENGTH_SHORT).show()
            return
        }

        val randomCocktail = matchingCocktails[Random.nextInt(matchingCocktails.size)]

        val intent = Intent(this, CocktailDetailActivity::class.java).apply {
            putExtra("cocktailId", randomCocktail.id)
            putExtra("cocktailName", randomCocktail.name)
            putExtra("cocktailGout", randomCocktail.flavourDescription)
            putExtra("cocktailHistory", randomCocktail.history)
            putExtra("cocktailExpertRating", randomCocktail.expertRating ?: 0.0)
            putExtra(
                "cocktailIngredients",
                ArrayList(randomCocktail.ingredients.map { "${it.quantity} ${it.name}" })
            )
        }
        startActivity(intent)
    }
}
