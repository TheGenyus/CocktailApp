package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.CheckBox
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.models.Party
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.random.Random

class CreatePartyActivity : AppCompatActivity() {

    private lateinit var inputMissingAllowed: EditText
    private lateinit var inputFilterIngredients: EditText
    private lateinit var inputFilterCocktails: com.google.android.material.textfield.TextInputEditText
    private lateinit var layoutIngredients: GridLayout
    private lateinit var layoutCocktails: LinearLayout
    private lateinit var tvCocktailInfo: android.widget.TextView
    private lateinit var btnCreate: android.widget.Button

    private val firestore = FirebaseFirestore.getInstance()
    private val allCocktails = mutableListOf<Cocktail>()
    private var baseIngredients: List<String> = emptyList()
    private val selectedIngredients = mutableSetOf<String>()
    private val selectedCocktailIds = mutableSetOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_party)

        inputMissingAllowed = findViewById(R.id.inputMissingAllowed)
        inputFilterIngredients = findViewById(R.id.inputFilterIngredients)
        inputFilterCocktails = findViewById(R.id.inputFilterCocktails)
        layoutIngredients = findViewById(R.id.layoutIngredients)
        layoutCocktails = findViewById(R.id.layoutCocktails)
        tvCocktailInfo = findViewById(R.id.tvCocktailInfo)
        btnCreate = findViewById(R.id.btnCreateParty)

        fetchCocktails()

        inputFilterIngredients.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderIngredients() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        inputFilterCocktails.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { renderCocktails() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnCreate.setOnClickListener { createParty() }
    }

    private fun fetchCocktails() {
        firestore.collection("cocktails").get()
            .addOnSuccessListener { snapshot ->
                val ingredientsSet = mutableSetOf<String>()
                snapshot.forEach { doc ->
                    val cocktail = doc.toObject(Cocktail::class.java)
                    val withId = if (cocktail.id.isBlank()) cocktail.copy(id = doc.id) else cocktail
                    allCocktails.add(withId)
                    ingredientsSet.addAll(withId.ingredients.map { it.name })
                }
                baseIngredients = listOf("Tous") + ingredientsSet.filter { it.isNotBlank() }.sorted()
                renderIngredients()
                renderCocktails()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.error_loading_cocktails_simple), Toast.LENGTH_SHORT).show()
            }
    }

    private fun renderIngredients() {
        layoutIngredients.removeAllViews()
        layoutIngredients.columnCount = 2

        val query = inputFilterIngredients.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isEmpty()) baseIngredients else {
            val others = baseIngredients.drop(1).filter { it.contains(query, ignoreCase = true) }
            listOf(baseIngredients.first()) + others
        }

        var allCheckBox: CheckBox? = null
        filtered.forEachIndexed { index, ingredient ->
            val cb = CheckBox(this).apply {
                text = ingredient
                setPadding(8)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(8, 8, 8, 8)
                }
                isChecked = if (index == 0) selectedIngredients.isEmpty() else selectedIngredients.contains(ingredient)
            }
            if (index == 0) {
                allCheckBox = cb
            }
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (cb == allCheckBox) {
                    if (isChecked) {
                        selectedIngredients.clear()
                        for (i in 0 until layoutIngredients.childCount) {
                            val child = layoutIngredients.getChildAt(i)
                            if (child is CheckBox && child != allCheckBox) child.isChecked = false
                        }
                    }
                } else {
                    if (isChecked) {
                        selectedIngredients.add(ingredient)
                        allCheckBox?.isChecked = false
                    } else {
                        selectedIngredients.remove(ingredient)
                        if (selectedIngredients.isEmpty()) allCheckBox?.isChecked = true
                    }
                }
                renderCocktails()
            }
            layoutIngredients.addView(cb)
        }
    }

    private fun allowedMissing(): Int {
        val raw = inputMissingAllowed.text?.toString()?.trim().orEmpty()
        return raw.toIntOrNull() ?: 0
    }

    private fun renderCocktails() {
        layoutCocktails.removeAllViews()
        val nameQuery = inputFilterCocktails.text?.toString()?.trim().orEmpty()
        val available = computeAvailableCocktails()
            .filter { cocktail ->
                nameQuery.isEmpty() || (cocktail.name?.contains(nameQuery, ignoreCase = true) == true)
            }
        tvCocktailInfo.text = "${available.size} cocktails disponibles"

        available.forEach { cocktail ->
            val cb = CheckBox(this).apply {
                text = cocktail.name ?: "Cocktail"
                isChecked = selectedCocktailIds.contains(cocktail.id)
                setPadding(8)
            }
            cb.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) selectedCocktailIds.add(cocktail.id) else selectedCocktailIds.remove(cocktail.id)
            }
            layoutCocktails.addView(cb)
        }
    }

    private fun computeAvailableCocktails(): List<Cocktail> {
        val availIngredients = if (selectedIngredients.isEmpty()) {
            null
        } else selectedIngredients.map { it.lowercase(Locale.getDefault()) }.toSet()

        val maxMissing = allowedMissing()
        return allCocktails.filter { cocktail ->
            val ingredientNames = cocktail.ingredients.map { it.name.lowercase(Locale.getDefault()) }
            val missing = if (availIngredients == null) 0 else ingredientNames.count { !availIngredients.contains(it) }
            missing <= maxMissing
        }
    }

    private fun generateCode(): String {
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet.random() }.joinToString("")
    }

    private fun createParty() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
            return
        }
        val cocktailsToSave = if (selectedCocktailIds.isEmpty()) computeAvailableCocktails().map { it.id } else selectedCocktailIds.toList()
        if (cocktailsToSave.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_aucun_cocktail), Toast.LENGTH_SHORT).show()
            return
        }

        val party = Party(
            code = generateCode(),
            ownerId = userId,
            barmen = listOf(userId),
            active = true,
            maxMissingIngredients = allowedMissing(),
            cocktailIds = cocktailsToSave,
            createdAt = System.currentTimeMillis()
        )

        firestore.collection("parties").document(party.code)
            .set(party)
            .addOnSuccessListener {
                Toast.makeText(this, getString(R.string.party_info_creee), Toast.LENGTH_SHORT).show()
                val intent = Intent(this, BarmanDashboardActivity::class.java)
                intent.putExtra("partyCode", party.code)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, getString(R.string.party_error_creation) + " : " + (e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }
}
