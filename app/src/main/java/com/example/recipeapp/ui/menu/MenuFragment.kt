package com.example.recipeapp.ui.menu

import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.recipeapp.FirebaseApiManager

import com.example.recipeapp.databinding.FragmentMenuBinding
import com.example.recipeapp.adapter.RecipeAdapter
import com.example.recipeapp.data.dao.AppDatabase
import com.example.recipeapp.data.dao.RecipeD
import com.example.recipeapp.data.model.Category
import com.example.recipeapp.data.model.Recipe
import com.example.recipeapp.ui.UiHelper
import com.example.recipeapp.ui.UiHelper.showRecipeDialog
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class MenuFragment : Fragment() {

    private lateinit var binding: FragmentMenuBinding
    private var recipeList = mutableListOf<Recipe>()
    private var categoryList = mutableListOf<Category>()
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

        loadRecipes()
       // recipeAdapter = RecipeAdapter(requireContext(), recipeList.toMutableList())
        categoryList = loadRecipeTypesFromAssets(requireContext()).map { Category(it.name) } // Convert from RecipeType to Category
            .toMutableList()
        recipeAdapter = RecipeAdapter(requireContext(), recipeList.toMutableList()) { selectedRecipe ->
            recipeToEdit = selectedRecipe
            updateExistingRecipe()
            Log.d("recipeToEdit", recipeToEdit?.id.toString() )
        }
        setupCategorySpinner()
        initializeRecyclerview()
        setupSearchListener()
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
                categoryFirstPicker()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
//
private fun categoryFirstPicker(){
    val categoryNames = categoryList.map { it.name }
    AlertDialog.Builder(requireContext())
        .setTitle("Choose Recipe Category")
        .setItems(categoryNames.toTypedArray()) { _, selectedIndex ->
            val selectedCategory = categoryNames[selectedIndex]
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val sampleRecipe = getSampleRecipeForCategory(requireContext(), selectedCategory)
                withContext(Dispatchers.Main) {
                    insertNewRecipe(sampleRecipe)
                }
            }

           // insertNewRecipe(sampleRecipe)
        }
        .setCancelable(true)
        .show()
}

    suspend fun getSampleRecipeForCategory(context: Context, category: String): Recipe {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "recipes_db"
        ).build()

       // val recipeDao = db.recipeDao()
        val inputStream = context.resources.openRawResource(R.raw.recipes) // OR from assets
        val jsonStr = inputStream.bufferedReader().use { it.readText() }

        val recipeList = mutableListOf<Recipe>()
       // val recipeListD = mutableListOf<RecipeD>()
        val jsonObject = JSONObject(jsonStr)
        val jsonArray = jsonObject.getJSONArray("Recipes")

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            val currentCategory = item.getString("category")

            val title = item.optString("title")
            val description = item.optString("description")
            val imageUrl = item.optString("imageUrl")

            val ingredientsJsonArray = item.optJSONArray("ingredients") ?: JSONArray()
            val ingredients = (0 until ingredientsJsonArray.length()).map {
                ingredientsJsonArray.getString(it)
            }
            val stepsJsonArray = item.optJSONArray("steps") ?: JSONArray()
            val steps = (0 until stepsJsonArray.length()).map {
                stepsJsonArray.getString(it)
            }
            //Dao
//            val itemD = jsonArray.getJSONObject(i)
//
//            val categoryD = itemD.getString("category")
//            val titleD = itemD.getString("title")
//            val descriptionD = itemD.getString("description")
//            val imageUrlD = itemD.getString("imageUrl")
//
//            val ingredientsD = itemD.getJSONArray("ingredients").let { arr ->
//                List(arr.length()) { arr.getString(it) }
//            }
//            val stepsD = itemD.getJSONArray("steps").let {arr ->
//            List(arr.length()) { arr.getString(it) }
//        }
//
//            val recipe = RecipeD(
//                category = categoryD,
//                title = titleD,
//                description = descriptionD,
//                ingredients = ingredientsD,
//                steps = stepsD,
//                imageUrl = imageUrlD
//            )

            recipeList.add(Recipe(category = currentCategory, title = title, description = description, ingredients = ingredients, steps = steps, imageUrl = imageUrl, isDraft = true))
           // recipeListD.add(recipe)

        }

     //   recipeDao.deleteAllRecipes()

    //        recipeListD.find { it.category.equals(category, ignoreCase = true) }
    //            ?.let { recipeDao.insertRecipe(it) }

     //   Log.d("Dao_get", recipeDao.getRecipeByCategory(category).toString())

        return recipeList.find { it.category.equals(category, ignoreCase = true) }
            ?: Recipe(title = "Recipe not found", category = category, isDraft = true)
    }


    private fun setupCategorySpinner() {
    val spinner = binding.categorySpinner
    val allCategories = mutableListOf("All")
  //  val categoryList: List<Category> = loadRecipeTypesFromAssets(requireContext())//UniversalPreferences.loadList(requireContext(), "categories")
    allCategories.addAll(categoryList.map { it.name })

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
   //val categoryList: List<Category> = loadRecipeTypesFromAssets(requireContext())//UniversalPreferences.loadList(requireContext(), "categories")
    recipeToEdit?.let { recipe ->
        showRecipeDialog(
            context = requireContext(),
            categories = categoryList,
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
            },
            onReload = {
              //  loadRecipes()
            }
        )
    } ?: Toast.makeText(requireContext(), "No recipe selected to edit.", Toast.LENGTH_SHORT).show()
}

private fun insertNewRecipe( sampleRecipe : Recipe) {
   // val categoryList: List<Category> = UniversalPreferences.loadList(requireContext(), "categories")
   // val nameList = categoryList.map { it.name } // convert to List<String>
    showRecipeDialog(
        context = requireContext(),
        categories = categoryList,
        initialRecipe = sampleRecipe,
        new = true,
        onConfirm = { newRecipe ->
            FirebaseApiManager().insertByKey(
                path = "Recipes",
                key = newRecipe.id.toString(),
                data = newRecipe,
                onSuccess = {
                    Toast.makeText(requireContext(), "Recipe added!", Toast.LENGTH_SHORT).show()

                },
                onError = {
                    Toast.makeText(requireContext(), "Error: ${it.message}", Toast.LENGTH_SHORT).show()
                })

        },
        onReload = {
            loadRecipes()
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
                if (recipeList.isNotEmpty()) {
                    UiHelper.showConfirmDialog(
                        context = requireContext(),
                        title = "Delete Recipe",
                        message = "Are you sure you want to delete \"${removed.title}\"?",
                        onConfirmed = {
                            // Proceed with deletion
                            recipeList.removeAt(position)
                            recipeAdapter.notifyItemRemoved(position)
                            if (removed.isDraft == false) {
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

                            } else {
                                val db = Room.databaseBuilder(
                                    requireContext(),
                                    AppDatabase::class.java,
                                    "recipes_db"
                                ).build()

                                val recipeDao = db.recipeDao()
                                CoroutineScope(Dispatchers.IO).launch {
                                    withContext(Dispatchers.Main) {
                                        recipeDao.deleteAllRecipes()
                                    }
                                }
                                loadRecipes()
                            }

                        },
                        onCancelled = {
                            // Restore the item visually if the user cancels
                            recipeAdapter.notifyItemChanged(position)
                        }
                    )
                }
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
/*
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

    }*/

    private fun loadRecipes() {

        FirebaseApiManager().loadListFromFirebase<Recipe>(
            dbPath = "Recipes",
            onLoaded = { loadedRecipes ->
                recipeList.clear()
                recipeList = loadedRecipes.toMutableList()
               try {

                   val db = Room.databaseBuilder(
                       requireContext(),
                       AppDatabase::class.java,
                       "recipes_db"
                   ).build()

                   val recipeDao = db.recipeDao()
                   CoroutineScope(Dispatchers.IO).launch {

                       val roomRecipes = recipeDao.getAllRecipes().toList() // this fetches from Room

                       withContext(Dispatchers.Main) {
                           // recipeList.clear()
                           if (roomRecipes.isNotEmpty()) {
                               val roomDrafts = roomRecipes
                                   .map { item ->
                                   Recipe(
                                       id = item.id.toString(),
                                       category = item.category,
                                       title = item.title,
                                       description = item.description,
                                       ingredients = item.ingredients,
                                       timestamp = 0,
                                       steps = item.steps,
                                       imageUrl = item.imageUrl,
                                       isDraft = true
                                   )

                               }
                               roomDrafts.forEach { item ->
                                   val exists = recipeList.any { it.id == item.id }
                                   if (!exists) {
                                       recipeList.add(item)
                                   }
                               }


                               Log.d("RecipeDao+Firebase", "Loaded Room drafts: $recipeList , ${recipeList.count()}")

                          } else {
                               Log.w("RecipeList", "No recipes found in database")
                               // Maybe show a UI message or fallback content
                           }

                    updateRecipeList(recipeList.distinctBy { it.id })
                    binding.loadingProgress.visibility = View.GONE
                   Log.d("recipe_list", "Loaded recipes:${recipeList.count()}")
                       }
                   }
               }catch (ex: Exception){
                   Log.d("dao_loadFailed", ex.toString())
               }
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
    fun loadRecipeTypesFromAssets(context: Context): List<Category> {
        val inputStream = context.resources.openRawResource(R.raw.recipetypes)
        val jsonStr = inputStream.bufferedReader().use { it.readText() }
        //val jsonStr = context.assets.open("recipetypes.json").bufferedReader().use { it.readText() }
        val recipeTypes = mutableListOf<Category>()

        val jsonObject = JSONObject(jsonStr)
        val jsonArray = jsonObject.getJSONArray("RecipeTypes")

        for (i in 0 until jsonArray.length()) {
            val item = jsonArray.getJSONObject(i)
            recipeTypes.add(Category(name = item.getString("name")))
        }
        Log.d("recipeType.json", recipeTypes.toString())
        return recipeTypes
    }

}
