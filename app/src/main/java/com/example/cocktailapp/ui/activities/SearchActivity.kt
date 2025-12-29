package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.data.CocktailRepository
import com.example.cocktailapp.models.Cocktail
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore

class SearchActivity : AppCompatActivity() {

    private lateinit var stepIngredients: LinearLayout
    private lateinit var stepSearch: LinearLayout
    private lateinit var nextButton: Button
    private lateinit var backButton: Button

    private lateinit var searchEditText: com.google.android.material.textfield.TextInputEditText
    private lateinit var ingredientFilterInput: TextInputEditText
    private lateinit var missingAllowedInput: TextInputEditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CocktailAdapter
    private lateinit var ingredientContainer: GridLayout
    private lateinit var checkboxAlcoholFree: CheckBox

    private val firestore = FirebaseFirestore.getInstance()
    private var allCocktails = listOf<Cocktail>()
    private val selectedIngredients = mutableSetOf<String>()
    private var baseIngredients: List<String> = emptyList()
    private lateinit var repo: CocktailRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searsh)

        repo = CocktailRepository(this)
        stepIngredients = findViewById(R.id.stepIngredients)
        stepSearch = findViewById(R.id.stepSearch)
        nextButton = findViewById(R.id.btnNextToSearch)
        backButton = findViewById(R.id.btnBackToIngredients)

        searchEditText = findViewById(R.id.searchEditText)
        ingredientFilterInput = findViewById(R.id.ingredientFilterInput)
        missingAllowedInput = findViewById(R.id.missingAllowedInput)
        recyclerView = findViewById(R.id.searchResultsRecyclerView)
        ingredientContainer = findViewById(R.id.ingredientContainer)
        checkboxAlcoholFree = findViewById(R.id.checkboxAlcoholFree)

        adapter = CocktailAdapter(allCocktails)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchAllCocktails()

        nextButton.setOnClickListener {
            stepIngredients.visibility = View.GONE
            stepSearch.visibility = View.VISIBLE
            applyFilters()
        }

        backButton.setOnClickListener {
            stepSearch.visibility = View.GONE
            stepIngredients.visibility = View.VISIBLE
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ingredientFilterInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterIngredientList() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        missingAllowedInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilters() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        checkboxAlcoholFree.setOnCheckedChangeListener { _, _ -> applyFilters() }
    }

    private fun fetchAllCocktails() {
        repo.fetchCocktails(onSuccess = { list ->
            val allIngredients = mutableSetOf<String>()
            list.forEach { cocktail ->
                cocktail.ingredients.forEach { ing ->
                    val name = ing.name.orEmpty().trim()
                    if (name.isNotEmpty()) {
                        allIngredients.add(name)
                    }
                }
            }
            allCocktails = list.filter { it.id.isNotBlank() }
            baseIngredients = listOf("Tous") + allIngredients.filter { it.isNotBlank() }.toList().sorted()
            setupIngredientButtons(baseIngredients)
            adapter.updateData(allCocktails)
        }, onError = {
            Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
        })
    }

    private fun filterIngredientList() {
        val query = ingredientFilterInput.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) {
            baseIngredients
        } else {
            val others = baseIngredients.drop(1).filter { it.contains(query, ignoreCase = true) }
            listOf(baseIngredients.first()) + others
        }
        setupIngredientButtons(filtered)
    }

    private fun setupIngredientButtons(ingredients: List<String>) {
        ingredientContainer.removeAllViews()
        ingredientContainer.columnCount = 1

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
                applyFilters()
            }

            ingredientContainer.addView(checkBox)
        }
    }

    private fun applyFilters() {
        val nameQuery = searchEditText.text?.toString()?.trim().orEmpty()
        val filterAlcoholFree = checkboxAlcoholFree.isChecked
        val allowedMissing = missingAllowedInput.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val selectedSet = selectedIngredients
            .filterNot { it.equals("Tous", ignoreCase = true) }
            .map { it.lowercase() }
            .toSet()

        val filtered = allCocktails.filter { cocktail ->
            val matchesName = nameQuery.isEmpty() || cocktail.name?.contains(nameQuery, ignoreCase = true) == true

            val cocktailNames = cocktail.ingredients
                .map { it.name.orEmpty().trim().lowercase() }
                .filter { it.isNotEmpty() }

            val matchesIngredients = if (selectedSet.isEmpty()) {
                true
            } else {
                val usesAtLeastOneSelected = cocktailNames.any { name ->
                    selectedSet.any { sel -> name.contains(sel, ignoreCase = true) }
                }
                val missingCount = cocktailNames.count { name ->
                    selectedSet.none { sel -> name.contains(sel, ignoreCase = true) }
                }
                usesAtLeastOneSelected && missingCount <= allowedMissing
            }

            val matchesAlcoholFree = if (!filterAlcoholFree) {
                true
            } else {
                val s = cocktail.strengthScore
                s != null && s == 0.0
            }
            matchesName && matchesIngredients && matchesAlcoholFree
        }
        adapter.updateData(filtered)
    }
}
