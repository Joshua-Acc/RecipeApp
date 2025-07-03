package com.example.recipeapp.ui.menu

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.example.recipeapp.R
import com.example.recipeapp.data.model.Recipe
import com.google.firebase.database.FirebaseDatabase

class FragmentAddRecipe : Fragment() {

    private lateinit var categorySpinner: Spinner
    private val categories = listOf("Pasta", "Salad", "Soup", "Dessert", "Main Course")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_recipe, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etDescription = view.findViewById<EditText>(R.id.etDescription)
        val etIngredients = view.findViewById<EditText>(R.id.etIngredients)
        val etSteps = view.findViewById<EditText>(R.id.etSteps)
        val etImageUrl = view.findViewById<EditText>(R.id.etImageUrl)
        val btnSubmit = view.findViewById<Button>(R.id.btnSubmit)
        categorySpinner = view.findViewById(R.id.spinnerCategory)

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = adapter

        btnSubmit.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val description = etDescription.text.toString().trim()
            val imageUrl = etImageUrl.text.toString().trim()
            val category = categorySpinner.selectedItem.toString()
            val ingredients = etIngredients.text.toString().split(",").map { it.trim() }
            val steps = etSteps.text.toString().split(",").map { it.trim() }

            val recipe = Recipe(
                category = category,
                createdBy = "U0000000007",
                description = description,
                imageUrl = imageUrl,
                ingredients = ingredients,
                steps = steps,
                title = title
            )

            val recipeRef = FirebaseDatabase.getInstance()
                .getReference("Recipes")
                .push()

            recipeRef.setValue(recipe)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Recipe added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}
