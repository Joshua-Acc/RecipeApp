package com.example.recipeapp.data.dao

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import com.example.recipeapp.data.model.Recipe
import com.google.firebase.Timestamp


@Entity(tableName = "recipes")
data class RecipeD(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val category: String,
    val title: String,
    val description: String,
    val ingredients: List<String>, // Can be stored as JSON or comma-separated
    val steps: List<String>,
    val imageUrl: String
)

@Dao
interface RecipeDao {
    @Query("SELECT * FROM recipes WHERE category = :category")
    suspend fun getRecipeByCategory(category: String): RecipeD?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeD)

    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipes(): List<RecipeD>

    @Query("DELETE FROM recipes WHERE category = :category")
    suspend fun deleteRecipesByCategory(category: String)

    @Query("DELETE FROM recipes WHERE title = :title")
    suspend fun deleteRecipesByTitle(title: String)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipesById(id: Long)

    @Query("DELETE FROM recipes")
    suspend fun deleteAllRecipes()
}




