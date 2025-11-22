package com.example.cocktailapp.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cocktailapp.R
import com.example.cocktailapp.models.Tutorial
import com.example.cocktailapp.ui.activities.TutorialDetailActivity

class TutorialAdapter(private var tutorialList: List<Tutorial>) :
    RecyclerView.Adapter<TutorialAdapter.TutorialViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TutorialViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tutorial, parent, false)
        return TutorialViewHolder(view)
    }

    override fun onBindViewHolder(holder: TutorialViewHolder, position: Int) {
        val tutorial = tutorialList[position]
        holder.bind(tutorial)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, TutorialDetailActivity::class.java).apply {
                putExtra("tutorialName", tutorial.name)
                putExtra("tutorialDescription", tutorial.description)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = tutorialList.size

    fun updateData(newTutorials: List<Tutorial>) {
        tutorialList = newTutorials
        notifyDataSetChanged()
    }

    class TutorialViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTutorialName: TextView = itemView.findViewById(R.id.tvTutorialName)

        fun bind(tutorial: Tutorial) {
            tvTutorialName.text = tutorial.name
        }
    }
}


