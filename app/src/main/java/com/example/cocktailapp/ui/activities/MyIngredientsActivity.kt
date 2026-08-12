package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.example.cocktailapp.R
import com.example.cocktailapp.data.CocktailRepository
import com.example.cocktailapp.data.MyIngredientsStore
import com.google.android.material.textfield.TextInputEditText

class MyIngredientsActivity : AppCompatActivity() {

    private lateinit var ingredientFilterInput: TextInputEditText
    private lateinit var ingredientContainer: GridLayout
    private lateinit var selectedCountText: TextView
    private lateinit var saveButton: Button
    private lateinit var clearButton: Button
    private lateinit var repo: CocktailRepository

    private var baseIngredients: List<String> = emptyList()
    private val selectedIngredients = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_ingredients)

        ingredientFilterInput = findViewById(R.id.myIngredientFilterInput)
        ingredientContainer = findViewById(R.id.myIngredientContainer)
        selectedCountText = findViewById(R.id.tvMyIngredientCount)
        saveButton = findViewById(R.id.btnSaveMyIngredients)
        clearButton = findViewById(R.id.btnClearMyIngredients)
        repo = CocktailRepository(this)

        selectedIngredients.addAll(MyIngredientsStore.load(this))
        updateSelectedCount()
        fetchIngredients()

        ingredientFilterInput.addTextChangedListener(simpleWatcher { renderIngredientCheckboxes() })
        saveButton.setOnClickListener {
            MyIngredientsStore.save(this, selectedIngredients)
            Toast.makeText(this, getString(R.string.info_my_ingredients_saved), Toast.LENGTH_SHORT).show()
            finish()
        }
        clearButton.setOnClickListener {
            selectedIngredients.clear()
            MyIngredientsStore.save(this, selectedIngredients)
            updateSelectedCount()
            renderIngredientCheckboxes()
        }
    }

    private fun simpleWatcher(after: () -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { after() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun fetchIngredients() {
        repo.fetchCocktails(onSuccess = { cocktails ->
            val ingredients = mutableSetOf<String>()
            cocktails.forEach { cocktail ->
                cocktail.ingredients.forEach { ingredient ->
                    val name = ingredient.name.orEmpty().trim()
                    if (name.isNotEmpty()) ingredients.add(name)
                }
            }
            baseIngredients = ingredients.sorted()
            renderIngredientCheckboxes()
        }, onError = {
            Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
        })
    }

    private fun renderIngredientCheckboxes() {
        ingredientContainer.removeAllViews()
        ingredientContainer.columnCount = 1

        val query = ingredientFilterInput.text?.toString()?.trim().orEmpty()
        val visibleIngredients = if (query.isEmpty()) {
            baseIngredients
        } else {
            baseIngredients.filter { it.contains(query, ignoreCase = true) }
        }

        visibleIngredients.forEach { ingredient ->
            val checkBox = CheckBox(this).apply {
                text = ingredient
                setPadding(8)
                isChecked = selectedIngredients.contains(ingredient)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                }
            }

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIngredients.add(ingredient)
                } else {
                    selectedIngredients.remove(ingredient)
                }
                updateSelectedCount()
            }

            ingredientContainer.addView(checkBox)
        }
    }

    private fun updateSelectedCount() {
        selectedCountText.text = getString(R.string.label_my_ingredients_count, selectedIngredients.size)
    }
}
