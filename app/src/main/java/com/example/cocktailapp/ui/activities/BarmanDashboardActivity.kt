package com.example.cocktailapp.ui.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.models.PartyOrder
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class BarmanDashboardActivity : AppCompatActivity() {

    private lateinit var tvCode: TextView
    private lateinit var btnAddCocktail: Button
    private lateinit var btnAddBarman: Button
    private lateinit var btnClose: Button
    private lateinit var ordersRecycler: RecyclerView

    private val firestore = FirebaseFirestore.getInstance()
    private var partyCode: String = ""
    private val orders = mutableListOf<PartyOrder>()
    private lateinit var adapter: OrdersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barman_dashboard)

        tvCode = findViewById(R.id.tvPartyCode)
        btnAddCocktail = findViewById(R.id.btnAddCocktail)
        btnAddBarman = findViewById(R.id.btnAddBarman)
        btnClose = findViewById(R.id.btnCloseParty)
        ordersRecycler = findViewById(R.id.ordersRecyclerView)

        adapter = OrdersAdapter(orders, ::updateStatus)
        ordersRecycler.layoutManager = LinearLayoutManager(this)
        ordersRecycler.adapter = adapter

        partyCode = intent.getStringExtra("partyCode") ?: ""
        if (partyCode.isBlank()) {
            Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        tvCode.text = getString(R.string.party_info_code, partyCode)

        listenOrders()

        btnAddCocktail.setOnClickListener { promptAddCocktail() }
        btnAddBarman.setOnClickListener { promptAddBarman() }
        btnClose.setOnClickListener { closeParty() }
    }

    private fun listenOrders() {
        firestore.collection("parties").document(partyCode)
            .collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                orders.clear()
                snapshot.forEach { doc ->
                    val order = doc.toObject(PartyOrder::class.java)
                    order.id = doc.id
                    // filtrer servi
                    if (order.status != "servi") {
                        orders.add(order)
                    }
                }
                orders.sortBy { it.createdAt }
                adapter.notifyDataSetChanged()
            }
    }

    private fun updateStatus(order: PartyOrder, newStatus: String) {
        val docRef = firestore.collection("parties").document(partyCode)
            .collection("orders").document(order.id)
        // Mettre a jour seulement (delete refuse par les regles), la liste cache "servi"
        docRef.update("status", newStatus)
            .addOnFailureListener {
                Toast.makeText(this, getString(R.string.party_error_creation), Toast.LENGTH_SHORT).show()
            }
    }

    private fun promptAddCocktail() {
        val input = EditText(this)
        input.hint = "ID du cocktail"
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.party_ajouter_cocktail))
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val id = input.text.toString().trim()
                if (id.isNotEmpty()) addCocktail(id)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun addCocktail(cocktailId: String) {
        firestore.collection("parties").document(partyCode)
            .update("cocktailIds", FieldValue.arrayUnion(cocktailId))
            .addOnSuccessListener { Toast.makeText(this, "Cocktail ajoute", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, getString(R.string.party_error_creation), Toast.LENGTH_SHORT).show() }
    }

    private fun promptAddBarman() {
        val input = EditText(this)
        input.hint = "UID du barman"
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.party_ajouter_barman))
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val id = input.text.toString().trim()
                if (id.isNotEmpty()) addBarman(id)
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    private fun addBarman(userId: String) {
        firestore.collection("parties").document(partyCode)
            .update("barmen", FieldValue.arrayUnion(userId))
            .addOnSuccessListener { Toast.makeText(this, "Barman ajoute", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, getString(R.string.party_error_creation), Toast.LENGTH_SHORT).show() }
    }

    private fun closeParty() {
        firestore.collection("parties").document(partyCode)
            .update("active", false)
            .addOnSuccessListener { Toast.makeText(this, "Soiree fermee", Toast.LENGTH_SHORT).show() }
            .addOnFailureListener { Toast.makeText(this, getString(R.string.party_error_creation), Toast.LENGTH_SHORT).show() }
    }

    class OrdersAdapter(
        private val items: List<PartyOrder>,
        private val onUpdate: (PartyOrder, String) -> Unit
    ) : RecyclerView.Adapter<OrdersAdapter.VH>() {
        class VH(view: View, val title: TextView, val subtitle: TextView, val btnPrep: Button, val btnServi: Button, val btnView: Button) : RecyclerView.ViewHolder(view)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val ctx = parent.context
            val root = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16)
                background = ContextCompat.getDrawable(ctx, R.color.bg_surface_variant)
            }
            val title = TextView(ctx).apply {
                textSize = 16f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
                setPadding(0, 0, 0, 4)
            }
            val subtitle = TextView(ctx).apply {
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
                setPadding(0, 0, 0, 8)
            }
            val buttons = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            val btnPrep = Button(ctx).apply { text = ctx.getString(R.string.party_statut_btn_preparation) }
            val btnServi = Button(ctx).apply { text = ctx.getString(R.string.party_statut_btn_servi) }
            val btnView = Button(ctx).apply { text = ctx.getString(R.string.label_voir_recette) }
            buttons.addView(btnPrep, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(btnServi, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            buttons.addView(btnView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            root.addView(title)
            root.addView(subtitle)
            root.addView(buttons)
            val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
            root.layoutParams = params
            return VH(root, title, subtitle, btnPrep, btnServi, btnView)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val order = items[position]
            holder.title.text = order.cocktailName.ifBlank { "Cocktail" }
            holder.subtitle.text = if (order.userName.isNotBlank()) "Par ${order.userName} - Statut : ${order.status}" else "Statut : ${order.status}"
            holder.btnPrep.setOnClickListener { onUpdate(order, "preparation") }
                        holder.btnServi.setOnClickListener { onUpdate(order, "servi") }
            holder.btnView.setOnClickListener {
                if (order.cocktailId.isNotBlank()) {
                    val intent = Intent(holder.itemView.context, CocktailDetailActivity::class.java)
                    intent.putExtra("cocktail_id", order.cocktailId)
                    holder.itemView.context.startActivity(intent)
                } else {
                    Toast.makeText(holder.itemView.context, R.string.error_cocktail_inconnu, Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun getItemCount(): Int = items.size
    }
}















