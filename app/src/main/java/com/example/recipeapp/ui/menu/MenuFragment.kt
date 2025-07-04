package com.example.recipeapp.ui.menu

import com.example.recipeapp.R

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeapp.FirebaseApiManager

import com.example.recipeapp.databinding.FragmentMenuBinding
import com.example.recipeapp.adapter.RecipeAdapter
import com.example.recipeapp.data.model.Category
import com.example.recipeapp.data.model.Recipe
import com.example.recipeapp.ui.UiHelper
import com.example.recipeapp.ui.UiHelper.showRecipeDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class MenuFragment : Fragment() {

    private lateinit var binding: FragmentMenuBinding
    private val recipeList = mutableListOf<Recipe>()
    private val categoryList = mutableListOf<Category>()
    private lateinit var recipeAdapter: RecipeAdapter
    private var recipeToEdit: Recipe? = null // 👈 declare it at class-level
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMenuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // recipeAdapter = RecipeAdapter(requireContext(), recipeList.toMutableList())

        recipeAdapter = RecipeAdapter(requireContext(), recipeList.toMutableList()) { selectedRecipe ->
            recipeToEdit = selectedRecipe
            updateExistingRecipe()
            Log.d("recipeToEdit", recipeToEdit?.id.toString() )
        }
        loadCategories()
        setupCategorySpinner()
        initializeRecyclerview()
        setupSearchListener()
        loadRecipes()
        setupSwipeToDelete()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.clear()
        inflater.inflate(R.menu.menu_main, menu)

        // To hide or show it conditionally:
        val insertItem = menu.findItem(R.id.action_insert)
        insertItem?.isVisible = true // or true depending on logic
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_insert -> {
                insertNewRecipe()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
//
private fun setupCategorySpinner() {
    val spinner = binding.categorySpinner
    val allCategories = mutableListOf("All")
    val categoryList: List<Category> = UniversalPreferences.loadList(requireContext(), "categories")
    allCategories.addAll(categoryList.firstOrNull()?.categoryList ?: emptyList())

    val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, allCategories)
    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)


    spinner.adapter = adapter

    spinner.setSelection(0) // default to "All"

    spinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>, view: View?, position: Int, id: Long) {
            filterRecipes()
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
    })
}


    private fun updateExistingRecipe() {
    val categoryList: List<Category> = UniversalPreferences.loadList(requireContext(), "categories")
    recipeToEdit?.let { recipe ->
        showRecipeDialog(
            context = requireContext(),
            categories = categoryList.first().categoryList,
            initialRecipe = recipe,
            onConfirm = { updatedRecipe ->

                val recipeId = updatedRecipe.id ?: recipe.id
                if (recipeId == null) {
                    Toast.makeText(requireContext(), "Missing recipe ID.", Toast.LENGTH_SHORT).show()
                    return@showRecipeDialog
                }

                FirebaseApiManager().saveDataToFirebase(
                    path = "Recipes/$recipeId",  // 👈 Use dynamic path
                    data = updatedRecipe,
                    onSuccess = {
                        Toast.makeText(requireContext(), "Recipe updated!", Toast.LENGTH_SHORT).show()
                        loadRecipes()
                    },
                    onError = {
                        Log.e("Firebase", "Failed to update recipe", it.toException())
                    }
                )
            }
        )
    } ?: Toast.makeText(requireContext(), "No recipe selected to edit.", Toast.LENGTH_SHORT).show()
}

private fun insertNewRecipe() {
    val categoryList: List<Category> = UniversalPreferences.loadList(requireContext(), "categories")
   // val nameList = categoryList.map { it.name } // convert to List<String>
    showRecipeDialog(
        context = requireContext(),
        categories = categoryList.first().categoryList,
        onConfirm = { newRecipe ->
            FirebaseApiManager().insertByKey(
                path = "Recipes",
                key = newRecipe.id.toString(),
                data = newRecipe,
                onSuccess = {
                    Toast.makeText(requireContext(), "Recipe added!", Toast.LENGTH_SHORT).show()
                     loadRecipes()
                },
                onError = {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                })

        }
    )
}
    private fun setupSwipeToDelete() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val removed = recipeList[position]

                // Show confirmation BEFORE removing from list
                UiHelper.showConfirmDialog(
                    context = requireContext(),
                    title = "Delete Recipe",
                    message = "Are you sure you want to delete \"${removed.title}\"?",
                    onConfirmed = {
                        // Proceed with deletion
                        recipeList.removeAt(position)
                        recipeAdapter.notifyItemRemoved(position)

                        FirebaseApiManager().deleteByFieldValue(
                            path = "Recipes",
                            field = "id",
                            value = removed.id.toString(),
                            onSuccess = {
                                Log.d("Firebase", "Deleted $it recipe(s)")
                            },
                            onError = {
                                Log.e("Firebase", "Error deleting: ${it.message}")
                            }
                        )
                    },
                    onCancelled = {
                        // Restore the item visually if the user cancels
                        recipeAdapter.notifyItemChanged(position)
                    }
                )
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.recyclerViewList)
    }


    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener {
            filterRecipes()
        }
    }
    private fun filterRecipes() {
        val query = binding.searchEditText.text.toString().trim()
        val selectedCategory = binding.categorySpinner.selectedItem?.toString() ?: "All"

        val filtered = recipeList.filter {
            val matchesCategory = selectedCategory == "All" || it.category.equals(selectedCategory, ignoreCase = true)
            val matchesQuery = it.title.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }

        updateRecipeList(filtered)
    }


    private fun updateRecipeList(displayList: List<Recipe>) {
        recipeAdapter.updateList(displayList)

        if (displayList.isEmpty()) {
            binding.recyclerViewList.visibility = View.GONE
            binding.emptyView.visibility = View.VISIBLE
        } else {
            binding.recyclerViewList.visibility = View.VISIBLE
            binding.emptyView.visibility = View.GONE
        }
    }


    private fun initializeRecyclerview(){
        binding.recyclerViewList.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewList.adapter = recipeAdapter
        binding.recyclerViewList.visibility = View.GONE
    }

    private fun loadRecipes() {
        FirebaseApiManager().loadListFromFirebase<Recipe>(
            dbPath = "Recipes",
            onLoaded = { loadedRecipes ->
                recipeList.clear()
                recipeList.addAll(loadedRecipes)
                Log.d("Firebase", "Loaded recipes: $loadedRecipes")
                binding.loadingProgress.visibility =View.GONE
                updateRecipeList(recipeList)
            },
            onError = { error ->
                Log.e("Firebase", "Failed to load recipes: ${error.message}")
            }
        )

    }

    private fun loadCategories() {
        FirebaseApiManager().loadListFromFirebase<Category>(
            dbPath = "Categories",
            onLoaded = { loadedCategories ->
                categoryList.clear()
                categoryList.addAll(loadedCategories)

                // Save the list using the generic version
                UniversalPreferences.saveList(requireContext(), "categories", loadedCategories)
                val categories: List<Category> = UniversalPreferences.loadList(requireContext(), "categories")

                Log.d("Firebase_Cat", "Loaded categories: $categories")
                binding.loadingProgress.visibility = View.GONE
            },
            onError = { error ->
                Log.e("Firebase", "Failed to load categories: ${error.message}")
            }
        )
    }

}
