package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class RandomActivity : AppCompatActivity() {

    private lateinit var ingredientContainer: GridLayout
    private lateinit var selectRandomButton: Button
    private lateinit var ingredientFilterInput: TextInputEditText
    private val selectedIngredients = mutableSetOf<String>()
    private val allCocktails = mutableListOf<Cocktail>()
    private var baseIngredients: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_cocktail_by_ingredient)

        ingredientContainer = findViewById(R.id.randomIngredientContainer)
        selectRandomButton = findViewById(R.id.btnSelectRandom)
        ingredientFilterInput = findViewById(R.id.randomIngredientFilterInput)

        fetchCocktails()

        selectRandomButton.setOnClickListener {
            selectRandomCocktail()
        }

        ingredientFilterInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterIngredients() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val contentLayout = findViewById<android.view.View>(R.id.contentLayout)
        val basePaddingBottom = contentLayout.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(contentLayout) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomPadding = systemInsets.bottom + basePaddingBottom
            view.updatePadding(top = systemInsets.top, bottom = bottomPadding)
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

                baseIngredients = listOf("Tous") + allIngredients.filter { it.isNotBlank() }.toList().sorted()
                renderIngredientCheckboxes(baseIngredients)
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterIngredients() {
        val query = ingredientFilterInput.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            baseIngredients
        } else {
            val others = baseIngredients.drop(1).filter { it.contains(query, ignoreCase = true) }
            listOf(baseIngredients.first()) + others
        }
        renderIngredientCheckboxes(filtered)
    }

    private fun renderIngredientCheckboxes(ingredients: List<String>) {
        ingredientContainer.columnCount = 2
        ingredientContainer.removeAllViews()

        var allCheckBox: CheckBox? = null

        ingredients.forEachIndexed { index, ingredient ->
            val checkBox = CheckBox(this).apply {
                text = ingredient
                setPadding(8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                }
            }

            if (index == 0) {
                allCheckBox = checkBox
                checkBox.isChecked = selectedIngredients.isEmpty()
            } else {
                checkBox.isChecked = selectedIngredients.contains(ingredient)
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (checkBox == allCheckBox) {
                    if (isChecked) {
                        selectedIngredients.clear()
                        for (i in 0 until ingredientContainer.childCount) {
                            val child = ingredientContainer.getChildAt(i)
                            if (child is CheckBox && child != allCheckBox) {
                                child.isChecked = false
                            }
                        }
                    }
                } else {
                    if (isChecked) {
                        selectedIngredients.add(ingredient)
                        allCheckBox?.isChecked = false
                    } else {
                        selectedIngredients.remove(ingredient)
                        if (selectedIngredients.isEmpty()) {
                            allCheckBox?.isChecked = true
                        }
                    }
                }
            }

            ingredientContainer.addView(checkBox)
        }
    }

    private fun selectRandomCocktail() {
        val matchingCocktails =
            if (selectedIngredients.isEmpty()) {
                allCocktails
            } else {
                allCocktails.filter { cocktail ->
                    cocktail.ingredients.any { selectedIngredients.contains(it.name) }
                }
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
