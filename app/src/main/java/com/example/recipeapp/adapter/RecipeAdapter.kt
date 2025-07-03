package com.example.recipeapp.adapter


import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.data.model.Recipe


class RecipeAdapter(
    private val context: Context,
    private var recipeList: List<Recipe>,
    private val onItemClicked: (Recipe) -> Unit // 👈 Add this!
) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image = itemView.findViewById<ImageView>(R.id.recipe_image)
        val title = itemView.findViewById<TextView>(R.id.recipe_title)
        val category = itemView.findViewById<TextView>(R.id.recipe_category)
        val description = itemView.findViewById<TextView>(R.id.recipe_description)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recipe_card_item, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.title.text = recipe.title
        holder.category.text = recipe.category
        holder.description.text = recipe.description
        Log.d("Adapter", "Binding recipe: ${recipe.title}")
        // Load image if available
       recipe.imageUrl?.let {
            Glide.with(context)
                .load(it)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.image)
        }
        holder.itemView.setOnClickListener {
            onItemClicked(recipe) // 👈 Invoke callback
        }
    }
    fun updateList(newList: List<Recipe>) {
        recipeList = newList.toMutableList()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = recipeList.size
}
