package com.example.cocktailapp.ui.activities

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.models.PartyOrder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PartyOrdersActivity : AppCompatActivity() {

    private lateinit var recycler: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()
    private val orders = mutableListOf<PartyOrder>()
    private lateinit var adapter: OrdersAdapter
    private var partyCode: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_party_orders)

        recycler = findViewById(R.id.ordersRecycler)
        adapter = OrdersAdapter(orders)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        partyCode = intent.getStringExtra("partyCode") ?: ""
        if (partyCode.isBlank()) {
            Toast.makeText(this, getString(R.string.party_error_code), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        listenOrders()
    }

    private fun listenOrders() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, getString(R.string.error_login_failed), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        firestore.collection("parties").document(partyCode)
            .collection("orders")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                orders.clear()
                snapshot.forEach { doc ->
                    val o = doc.toObject(PartyOrder::class.java)
                    o.id = doc.id
                    orders.add(o)
                }
                orders.sortBy { it.createdAt }
                adapter.notifyDataSetChanged()
            }
    }

    class OrdersAdapter(private val items: List<PartyOrder>) : RecyclerView.Adapter<OrdersAdapter.VH>() {
        class VH(view: View, val title: TextView, val subtitle: TextView) : RecyclerView.ViewHolder(view)

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
            root.addView(title)
            root.addView(subtitle)
            val params = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 8, 0, 8)
            }
            root.layoutParams = params
            return VH(root, title, subtitle)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val order = items[position]
            holder.title.text = order.cocktailName.ifBlank { "Cocktail" }
            holder.subtitle.text = "Statut : ${order.status}"
        }

        override fun getItemCount(): Int = items.size
    }
}
