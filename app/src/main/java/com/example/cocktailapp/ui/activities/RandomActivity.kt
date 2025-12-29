package com.example.cocktailapp.ui.activities

import android.content.Intent
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.setPadding
import androidx.core.view.updatePadding
import com.example.cocktailapp.R
import com.example.cocktailapp.data.CocktailRepository
import com.example.cocktailapp.models.Cocktail
import com.google.android.material.textfield.TextInputEditText
import kotlin.random.Random

class RandomActivity : AppCompatActivity() {

    private lateinit var ingredientContainer: GridLayout
    private lateinit var ingredientFilterInput: TextInputEditText
    private lateinit var selectRandomButton: Button
    private lateinit var backButton: Button
    private lateinit var filtersBlock: LinearLayout
    private lateinit var checkboxAlcoholFree: CheckBox
    private lateinit var inputStrengthMin: TextInputEditText
    private lateinit var inputSweetMin: TextInputEditText
    private lateinit var inputExpertMin: TextInputEditText
    private lateinit var inputMemberMin: TextInputEditText

    private val selectedIngredients = mutableSetOf<String>()
    private val allCocktails = mutableListOf<Cocktail>()
    private var baseIngredients: List<String> = emptyList()
    private lateinit var repo: CocktailRepository

    private lateinit var stepIngredients: LinearLayout
    private lateinit var stepFilters: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_random_cocktail_by_ingredient)

        ingredientContainer = findViewById(R.id.randomIngredientContainer)
        ingredientFilterInput = findViewById(R.id.randomIngredientFilterInput)
        selectRandomButton = findViewById(R.id.btnGenerateRandom)
        backButton = findViewById(R.id.btnBackToIngredientsRandom)
        filtersBlock = findViewById(R.id.filtersBlock)
        checkboxAlcoholFree = findViewById(R.id.checkboxRandomAlcoholFree)
        inputStrengthMin = findViewById(R.id.inputRandomStrengthMin)
        inputSweetMin = findViewById(R.id.inputRandomSweetMin)
        inputExpertMin = findViewById(R.id.inputRandomExpertMin)
        inputMemberMin = findViewById(R.id.inputRandomMemberMin)
        stepIngredients = findViewById(R.id.stepIngredients)
        stepFilters = findViewById(R.id.stepFilters)
        val nextButton = findViewById<Button>(R.id.btnNextToFilters)
        repo = CocktailRepository(this)

        fetchCocktails()

        nextButton.setOnClickListener {
            stepIngredients.visibility = View.GONE
            stepFilters.visibility = View.VISIBLE
            filtersBlock.visibility = View.VISIBLE
        }

        backButton.setOnClickListener {
            stepFilters.visibility = View.GONE
            stepIngredients.visibility = View.VISIBLE
        }

        selectRandomButton.setOnClickListener {
            filtersBlock.visibility = View.GONE
            selectRandomCocktail()
        }

        ingredientFilterInput.addTextChangedListener(simpleWatcher { filterIngredients() })

        val contentLayout = findViewById<android.view.View>(R.id.contentLayout)
        val basePaddingBottom = contentLayout.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(contentLayout) { view, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val bottomPadding = systemInsets.bottom + basePaddingBottom
            view.updatePadding(top = systemInsets.top, bottom = bottomPadding)
            insets
        }
    }

    private fun simpleWatcher(after: () -> Unit) = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) { after() }
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
    }

    private fun fetchCocktails() {
        repo.fetchCocktails(onSuccess = { list ->
            allCocktails.clear()
            allCocktails.addAll(list.filter { it.id.isNotBlank() })
            val allIngredients = mutableSetOf<String>()
            allCocktails.forEach { cocktail ->
                cocktail.ingredients.forEach { ing ->
                    val name = ing.name.orEmpty()
                    if (name.isNotBlank()) allIngredients.add(name)
                }
            }
            baseIngredients = listOf("Tous") + allIngredients.filter { it.isNotBlank() }.toList().sorted()
            renderIngredientCheckboxes(baseIngredients)
        }, onError = {
            Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
        })
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
        ingredientContainer.columnCount = 1
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

    private fun parseDouble(editText: TextInputEditText): Double? =
        editText.text?.toString()?.replace(",", ".")?.toDoubleOrNull()

    private fun minOk(value: Double?, min: Double?): Boolean {
        if (min == null) return true
        if (value == null) return true
        return value >= min
    }

    private fun selectRandomCocktail() {
        val selectedSet = selectedIngredients
            .filterNot { it.equals("Tous", ignoreCase = true) }
            .map { it.lowercase() }
            .toSet()

        val minStrength = parseDouble(inputStrengthMin)
        val minSweet = parseDouble(inputSweetMin)
        val minExpert = parseDouble(inputExpertMin)
        val minMember = parseDouble(inputMemberMin)
        val alcoholFreeOnly = checkboxAlcoholFree.isChecked

        val matchingCocktails = allCocktails.filter { cocktail ->
            val cocktailNames = cocktail.ingredients
                .map { it.name.orEmpty().trim().lowercase() }
                .filter { it.isNotEmpty() }

            val matchesIngredients = if (selectedSet.isEmpty()) {
                true
            } else {
                cocktailNames.any { name -> selectedSet.any { sel -> name.contains(sel, ignoreCase = true) } }
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

            matchesIngredients && strengthOk && sweetOk && expertOk && memberOk && alcoholOk
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
