package com.example.cocktailapp.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Cocktail
import com.example.cocktailapp.ui.activities.CocktailDetailActivity

class CocktailAdapter(private var cocktailList: List<Cocktail>) :
    RecyclerView.Adapter<CocktailAdapter.CocktailViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CocktailViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_cocktail, parent, false)
        return CocktailViewHolder(view)
    }

    override fun onBindViewHolder(holder: CocktailViewHolder, position: Int) {
        val cocktail = cocktailList[position]
        holder.bind(cocktail)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, CocktailDetailActivity::class.java).apply {
                putExtra("cocktailId", cocktail.id)
                putExtra("cocktailName", cocktail.name)
                putExtra("cocktailGout", cocktail.flavourDescription)
                putExtra("cocktailHistory", cocktail.history)
                putExtra("cocktailExpertRating", cocktail.expertRating ?: 0.0)
                putExtra(
                    "cocktailIngredients",
                    ArrayList(cocktail.ingredients.map { "${it.quantity} ${it.name}" })
                )
            }
            context.startActivity(intent)
        }
    }

    fun updateData(newCocktails: List<Cocktail>) {
        cocktailList = newCocktails
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = cocktailList.size

    class CocktailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvCocktailName: TextView = itemView.findViewById(R.id.tvCocktailName)

        fun bind(cocktail: Cocktail) {
            tvCocktailName.text = cocktail.name
        }
    }
}
