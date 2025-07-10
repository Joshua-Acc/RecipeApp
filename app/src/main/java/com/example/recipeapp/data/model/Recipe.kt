package com.example.recipeapp.data.model

data class Recipe(
    val id: String? = null,               // Unique ID for the recipe (optional for Firebase push())
    val title: String = "",               // Title of the recipe
    val description: String = "",         // Brief summary or intro
    val ingredients: List<String> = listOf(), // List of ingredients
    val steps: List<String> = listOf(),       // Step-by-step instructions
    val category: String = "",            // Recipe category (e.g. "Dessert", "Main")
    val imageUrl: String? = null,         // Optional URL to a recipe image
    val timestamp: Long = System.currentTimeMillis(), // For sorting/filtering
    val createdBy: String? = null  ,       // UserID or email who created it (optional)
    val isDraft: Boolean = false
)
