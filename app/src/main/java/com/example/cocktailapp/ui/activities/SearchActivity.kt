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
import com.example.cocktailapp.data.MyIngredientsStore
import com.example.cocktailapp.models.Cocktail
import com.google.android.material.textfield.TextInputEditText

class SearchActivity : AppCompatActivity() {

    private lateinit var stepIngredientChoice: LinearLayout
    private lateinit var stepIngredients: LinearLayout
    private lateinit var stepSearch: LinearLayout
    private lateinit var useMyIngredientsButton: Button
    private lateinit var chooseIngredientListButton: Button
    private lateinit var nextButton: Button
    private lateinit var backToIngredientChoiceButton: Button
    private lateinit var backButton: Button
    private lateinit var showResultsButton: Button
    private lateinit var filtersBlock: LinearLayout

    private lateinit var searchEditText: com.google.android.material.textfield.TextInputEditText
    private lateinit var ingredientFilterInput: TextInputEditText
    private lateinit var missingAllowedInput: TextInputEditText
    private lateinit var inputStrengthMin: TextInputEditText
    private lateinit var inputSweetMin: TextInputEditText
    private lateinit var inputExpertMin: TextInputEditText
    private lateinit var inputMemberMin: TextInputEditText
    private lateinit var checkboxAlcoholFree: CheckBox

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: CocktailAdapter
    private lateinit var ingredientContainer: GridLayout

    private var allCocktails = listOf<Cocktail>()
    private val selectedIngredients = mutableSetOf<String>()
    private var baseIngredients: List<String> = emptyList()
    private lateinit var repo: CocktailRepository

    private var resultsVisible = false
    private var usingMyIngredients = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searsh)

        repo = CocktailRepository(this)
        stepIngredientChoice = findViewById(R.id.stepIngredientChoice)
        stepIngredients = findViewById(R.id.stepIngredients)
        stepSearch = findViewById(R.id.stepSearch)
        useMyIngredientsButton = findViewById(R.id.btnUseMyIngredients)
        chooseIngredientListButton = findViewById(R.id.btnChooseIngredientList)
        nextButton = findViewById(R.id.btnNextToSearch)
        backToIngredientChoiceButton = findViewById(R.id.btnBackToIngredientChoice)
        backButton = findViewById(R.id.btnBackToIngredients)
        showResultsButton = findViewById(R.id.btnShowResults)
        filtersBlock = findViewById(R.id.filtersBlock)

        searchEditText = findViewById(R.id.searchEditText)
        ingredientFilterInput = findViewById(R.id.ingredientFilterInput)
        missingAllowedInput = findViewById(R.id.missingAllowedInput)
        inputStrengthMin = findViewById(R.id.inputStrengthMin)
        inputSweetMin = findViewById(R.id.inputSweetMin)
        inputExpertMin = findViewById(R.id.inputExpertMin)
        inputMemberMin = findViewById(R.id.inputMemberMin)
        checkboxAlcoholFree = findViewById(R.id.checkboxAlcoholFree)
        recyclerView = findViewById(R.id.searchResultsRecyclerView)
        ingredientContainer = findViewById(R.id.ingredientContainer)

        adapter = CocktailAdapter(allCocktails)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        fetchAllCocktails()

        useMyIngredientsButton.setOnClickListener {
            val myIngredients = MyIngredientsStore.load(this)
            if (myIngredients.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_no_my_ingredients), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            usingMyIngredients = true
            selectedIngredients.clear()
            selectedIngredients.addAll(myIngredients)
            showSearchStep()
        }

        chooseIngredientListButton.setOnClickListener {
            usingMyIngredients = false
            selectedIngredients.clear()
            ingredientFilterInput.text = null
            setupIngredientButtons(baseIngredients)
            stepIngredientChoice.visibility = View.GONE
            stepIngredients.visibility = View.VISIBLE
        }

        nextButton.setOnClickListener {
            showSearchStep()
        }

        backToIngredientChoiceButton.setOnClickListener {
            stepIngredients.visibility = View.GONE
            stepIngredientChoice.visibility = View.VISIBLE
        }

        backButton.setOnClickListener {
            stepSearch.visibility = View.GONE
            if (usingMyIngredients) {
                stepIngredientChoice.visibility = View.VISIBLE
            } else {
                stepIngredients.visibility = View.VISIBLE
            }
        }

        showResultsButton.setOnClickListener {
            resultsVisible = true
            filtersBlock.visibility = View.GONE
            showResultsButton.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            applyFilters()
        }

        searchEditText.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        ingredientFilterInput.addTextChangedListener(simpleWatcher { filterIngredientList() })
        missingAllowedInput.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        inputStrengthMin.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        inputSweetMin.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        inputExpertMin.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        inputMemberMin.addTextChangedListener(simpleWatcher { if (resultsVisible) applyFilters() })
        checkboxAlcoholFree.setOnCheckedChangeListener { _, _ -> if (resultsVisible) applyFilters() }
    }

    private fun showSearchStep() {
        stepIngredientChoice.visibility = View.GONE
        stepIngredients.visibility = View.GONE
        stepSearch.visibility = View.VISIBLE
        resultsVisible = false
        recyclerView.visibility = View.GONE
        filtersBlock.visibility = View.VISIBLE
        showResultsButton.visibility = View.VISIBLE
        adapter.updateData(emptyList())
    }

    private fun simpleWatcher(after: () -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { after() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
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
                if (resultsVisible) applyFilters()
            }

            ingredientContainer.addView(checkBox)
        }
    }

    private fun parseDouble(editText: TextInputEditText): Double? {
        return editText.text?.toString()?.replace(",", ".")?.toDoubleOrNull()
    }

    private fun minOk(value: Double?, min: Double?): Boolean {
        if (min == null) return true
        if (value == null) return true // info manquante : on laisse passer
        return value >= min
    }

    private fun applyFilters() {
        if (!resultsVisible) {
            adapter.updateData(emptyList())
            return
        }

        val nameQuery = searchEditText.text?.toString()?.trim().orEmpty()
        val allowedMissing = missingAllowedInput.text?.toString()?.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val selectedSet = selectedIngredients
            .filterNot { it.equals("Tous", ignoreCase = true) }
            .map { it.lowercase() }
            .toSet()

        val minStrength = parseDouble(inputStrengthMin)
        val minSweet = parseDouble(inputSweetMin)
        val minExpert = parseDouble(inputExpertMin)
        val minMember = parseDouble(inputMemberMin)
        val alcoholFreeOnly = checkboxAlcoholFree.isChecked

        val filtered = allCocktails.filter { cocktail ->
            val matchesName = nameQuery.isEmpty() || cocktail.name?.contains(nameQuery, ignoreCase = true) == true

            val cocktailNames = cocktail.ingredients
                .map { it.name.orEmpty().trim().lowercase() }
                .filter { it.isNotEmpty() }

            val matchesIngredients = if (selectedSet.isEmpty()) {
                true
            } else {
                val missingCount = cocktailNames.count { name ->
                    selectedSet.none { sel -> name.contains(sel, ignoreCase = true) }
                }
                if (usingMyIngredients) {
                    missingCount <= allowedMissing
                } else {
                    val usesAtLeastOneSelected = cocktailNames.any { name ->
                        selectedSet.any { sel -> name.contains(sel, ignoreCase = true) }
                    }
                    usesAtLeastOneSelected && missingCount <= allowedMissing
                }
            }

            val strengthOk = minOk(cocktail.strengthScore, minStrength)
            val sweetOk = minOk(cocktail.tasteScore, minSweet)
            val expertOk = minOk(cocktail.expertRating?.toDouble(), minExpert)
            val memberOk = minOk(cocktail.memberRating?.toDouble(), minMember)
            val alcoholOk = if (!alcoholFreeOnly) {
                true
            } else {
                val s = cocktail.strengthScore
                s != null && s == 0.0
            }

            matchesName && matchesIngredients && strengthOk && sweetOk && expertOk && memberOk && alcoholOk
        }
        adapter.updateData(filtered)
    }
}
