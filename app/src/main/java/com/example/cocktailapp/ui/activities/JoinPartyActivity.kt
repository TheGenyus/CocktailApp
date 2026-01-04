package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.adapters.CocktailAdapter
import com.example.cocktailapp.data.CocktailRepository
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.models.Party
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

class JoinPartyActivity : AppCompatActivity() {

    private lateinit var inputCode: TextInputEditText
    private lateinit var btnJoin: Button
    private lateinit var btnLeave: Button
    private lateinit var btnShowCocktails: Button
    private lateinit var btnShowOrders: Button
    private lateinit var btnBarmanDashboard: Button
    private lateinit var tvStatus: TextView
    private lateinit var recycler: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var repo: CocktailRepository
    private var currentParty: Party? = null
    private val cocktails = mutableListOf<Cocktail>()
    private lateinit var adapter: CocktailAdapter
    private val prefs by lazy { getSharedPreferences("party_prefs", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_party_clean)

        inputCode = findViewById(R.id.inputPartyCode)
        btnJoin = findViewById(R.id.btnJoin)
        btnLeave = findViewById(R.id.btnLeaveParty)
        btnShowCocktails = findViewById(R.id.btnShowCocktails)
        btnShowOrders = findViewById(R.id.btnShowOrders)
        btnBarmanDashboard = findViewById(R.id.btnBarmanDashboard)
        tvStatus = findViewById(R.id.tvPartyStatus)
        recycler = findViewById(R.id.partyCocktailList)

        repo = CocktailRepository(this)
        adapter = CocktailAdapter(cocktails, partyCode = null)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnJoin.setOnClickListener { joinParty() }
        btnLeave.setOnClickListener {
            currentParty = null
            cocktails.clear()
            adapter.updateData(cocktails)
            adapter.setPartyCode(null)
            btnLeave.visibility = View.GONE
            btnShowCocktails.visibility = View.GONE
            btnShowOrders.visibility = View.GONE
            btnBarmanDashboard.visibility = View.GONE
            recycler.visibility = View.GONE
            btnJoin.visibility = View.VISIBLE
            inputCode.isEnabled = true
            tvStatus.text = ""
        }

        btnShowCocktails.setOnClickListener {
            recycler.visibility = if (recycler.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        btnShowOrders.setOnClickListener {
            currentParty?.let {
                val intent = Intent(this, PartyOrdersActivity::class.java)
                intent.putExtra("partyCode", it.code)
                startActivity(intent)
            }
        }
    }

    private fun joinParty() {
        val code = inputCode.text?.toString()?.trim().orEmpty().uppercase()
        if (code.length != 8) {
            Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection("parties").document(code).get()
            .addOnSuccessListener { doc ->
                val party = doc.toObject(Party::class.java)
                if (party == null || !party.active) {
                    Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                currentParty = party
                adapter.setPartyCode(party.code)
                tvStatus.text = getString(R.string.party_info_code, code)
                btnLeave.visibility = View.VISIBLE
                btnJoin.visibility = View.GONE
                inputCode.isEnabled = false
                btnShowCocktails.visibility = View.VISIBLE
                btnShowOrders.visibility = View.VISIBLE
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                val isBarman = uid != null && party.barmen.contains(uid)
                btnBarmanDashboard.visibility = if (isBarman) View.VISIBLE else View.GONE
                btnBarmanDashboard.setOnClickListener {
                    val intent = Intent(this, BarmanDashboardActivity::class.java)
                    intent.putExtra("partyCode", party.code)
                    startActivity(intent)
                }
                loadCocktailsForParty(party)
                recycler.visibility = View.VISIBLE
                ensureDisplayName()
                Toast.makeText(this, getString(R.string.party_info_rejointe), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
            }
    }

    private fun ensureDisplayName() {
        val current = prefs.getString("display_name", "") ?: ""
        if (current.isNotBlank()) return

        val edit = TextInputEditText(this)
        edit.hint = getString(R.string.label_nom_inconnu)
        edit.setText(current)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.party_prompt_name))
            .setView(edit)
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val name = edit.text?.toString()?.trim().orEmpty()
                if (name.isBlank()) {
                    Toast.makeText(this, getString(R.string.party_error_name), Toast.LENGTH_SHORT).show()
                    ensureDisplayName()
                } else {
                    prefs.edit().putString("display_name", name).apply()
                    dialog.dismiss()
                }
            }
            .show()
    }

    private fun loadCocktailsForParty(party: Party) {
        cocktails.clear()
        val ids = party.cocktailIds.toSet()
        if (ids.isEmpty()) {
            adapter.updateData(cocktails)
            return
        }
        repo.fetchCocktails(onSuccess = { list ->
            list.forEach { c -> if (!c.id.isNullOrBlank() && ids.contains(c.id)) cocktails.add(c) }
            adapter.updateData(cocktails)
        }, onError = {
            Toast.makeText(this, getString(R.string.party_error_chargement), Toast.LENGTH_SHORT).show()
        })
    }
}




