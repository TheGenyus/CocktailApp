package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.models.Party
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class JoinPartyActivity : AppCompatActivity() {

    private lateinit var inputCode: com.google.android.material.textfield.TextInputEditText
    private lateinit var btnJoin: Button
    private lateinit var btnLeave: Button
    private lateinit var btnShowCocktails: Button
    private lateinit var btnShowOrders: Button
    private lateinit var tvStatus: TextView
    private lateinit var recycler: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private var currentParty: Party? = null
    private val cocktails = mutableListOf<Cocktail>()
    private lateinit var adapter: SimpleCocktailAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_join_party)

        inputCode = findViewById(R.id.inputPartyCode)
        btnJoin = findViewById(R.id.btnJoin)
        btnLeave = findViewById(R.id.btnLeaveParty)
        btnShowCocktails = findViewById(R.id.btnShowCocktails)
        btnShowOrders = findViewById(R.id.btnShowOrders)
        tvStatus = findViewById(R.id.tvPartyStatus)
        recycler = findViewById(R.id.partyCocktailList)

        adapter = SimpleCocktailAdapter(cocktails) { cocktail ->
            val intent = Intent(this, CocktailDetailActivity::class.java)
            intent.putExtra("cocktailId", cocktail.id)
            currentParty?.let { intent.putExtra("partyCode", it.code) }
            startActivity(intent)
        }
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        btnJoin.setOnClickListener { joinParty() }
        btnLeave.setOnClickListener {
            currentParty = null
            cocktails.clear()
            adapter.notifyDataSetChanged()
            btnLeave.visibility = View.GONE
            btnShowCocktails.visibility = View.GONE
            btnShowOrders.visibility = View.GONE
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
                tvStatus.text = getString(R.string.party_info_code, code)
                btnLeave.visibility = View.VISIBLE
                btnJoin.visibility = View.GONE
                inputCode.isEnabled = false
                btnShowCocktails.visibility = View.VISIBLE
                btnShowOrders.visibility = View.VISIBLE
                loadCocktailsForParty(party)
                Toast.makeText(this, getString(R.string.party_info_rejointe), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadCocktailsForParty(party: Party) {
        cocktails.clear()
        val ids = party.cocktailIds.toSet()
        if (ids.isEmpty()) {
            adapter.notifyDataSetChanged()
            return
        }
        firestore.collection("cocktails").get()
            .addOnSuccessListener { snapshot ->
                snapshot.forEach { doc ->
                    val c = doc.toObject(Cocktail::class.java)
                    val withId = if (c.id.isBlank()) c.copy(id = doc.id) else c
                    if (ids.contains(withId.id)) cocktails.add(withId)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.party_error_chargement), Toast.LENGTH_SHORT).show()
            }
    }

    class SimpleCocktailAdapter(
        private val items: List<Cocktail>,
        private val onClick: (Cocktail) -> Unit
    ) : RecyclerView.Adapter<SimpleCocktailAdapter.VH>() {
        class VH(view: View, val name: TextView) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val container = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(12)
            }
            val tv = TextView(ctx).apply {
                textSize = 16f
                setTextColor(ctx.getColor(R.color.text_primary))
            }
            container.addView(tv)
            return VH(container, tv)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val c = items[position]
            holder.name.text = c.name ?: "Cocktail"
            holder.itemView.setOnClickListener { onClick(c) }
        }

        override fun getItemCount(): Int = items.size
    }
}
