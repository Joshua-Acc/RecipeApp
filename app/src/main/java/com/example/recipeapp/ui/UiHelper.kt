package com.example.recipeapp.ui


import androidx.lifecycle.lifecycleScope

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnFocusChangeListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.bumptech.glide.Glide
import com.example.recipeapp.R
import com.example.recipeapp.data.dao.AppDatabase
import com.example.recipeapp.data.dao.RecipeD
import com.example.recipeapp.data.model.Category
import com.example.recipeapp.data.model.Recipe
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


object UiHelper {
    fun Button.disableTemporarily(durationMillis: Long = 2000L) {
        isEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({
            isEnabled = true
        }, durationMillis)
    }


    fun View.applyTextAndBackgroundStyle(
        textColorHex: String = "#B3FFFFFF", // default: white with 70% opacity
        backgroundAlpha: Float = 0.7f       // expressed as 0.0 to 1.0
    ) {
        if (this is TextView) {
            setTextColor(Color.parseColor(textColorHex))
        }
        background?.alpha = (backgroundAlpha * 255).toInt()  // Convert to 0–255 range
    }


    fun AppCompatActivity.handleBackPressWithAuthCheck() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (FirebaseAuth.getInstance().currentUser == null) {
                    finish() // Exit app if user not logged in
                } else {
                    moveTaskToBack(true) // Minimize app to prevent back nav
                }
            }
        })
    }

    fun showConfirmDialog(
        context: Context,
        title: String,
        message: String,
        positiveText: String = "Yes",
        negativeText: String = "Cancel",
        onConfirmed: () -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(positiveText) { _, _ -> onConfirmed() }
            .setNegativeButton(negativeText) { _, _ -> onCancelled() }
            .setCancelable(false)
            .show()
    }

    fun showRecipeDialog(
        context: Context,
        categories: List<Category>,
        initialRecipe: Recipe? = null,
        new: Boolean? = false,
        onConfirm: (Recipe) -> Unit,
        onReload : () -> Unit
    ) {
        val db = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "recipes_db"
        ).build()

        val dialogView = LayoutInflater.from(context).inflate(R.layout.fragment_add_recipe, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etTitle)
        val etDescription = dialogView.findViewById<EditText>(R.id.etDescription)
        val etIngredients = dialogView.findViewById<EditText>(R.id.etIngredients)
        val etSteps = dialogView.findViewById<EditText>(R.id.etSteps)
        val etImageUrl = dialogView.findViewById<EditText>(R.id.etImageUrl)
        val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btnSubmit)
        val btnDraft = dialogView.findViewById<Button>(R.id.btnSaveDraft)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        val categoryNames = categories.map { it.name }
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        var id = ""



        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, categoryNames)
        spinnerCategory.adapter = adapter
        btnDraft.isVisible = initialRecipe?.isDraft == true
        initialRecipe?.let { recipe ->
            etTitle.setText(recipe.title)
            etDescription.setText(recipe.description)
            etIngredients.setText(recipe.ingredients.joinToString(", "))
            etSteps.setText(recipe.steps.joinToString(", "))
            etImageUrl.setText(recipe.imageUrl ?: "")
            val selectedIndex = categoryNames.indexOf(recipe.category)
            if (selectedIndex >= 0) spinnerCategory.setSelection(selectedIndex)
            btnSubmit.text = if (initialRecipe.isDraft == false)"Update" else "Add"
            val title = etTitle.text.toString()
            val firstChar = title.firstOrNull()?.toString() ?: "X"
            id =  if(new != true) initialRecipe.id.toString() else firstChar + UUID.randomUUID().toString()
        }

        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        btnSubmit.setOnClickListener {
            val recipe = Recipe(
                id = id ,
                title = etTitle.text.toString(),
                description = etDescription.text.toString(),
                ingredients = etIngredients.text.toString().split(',').map { it.trim() },
                steps = etSteps.text.toString().split(',').map { it.trim() },
                category = spinnerCategory.selectedItem.toString(),
                imageUrl = etImageUrl.text.toString(),
                createdBy = initialRecipe?.createdBy,
                timestamp = initialRecipe?.timestamp ?: System.currentTimeMillis(),
                isDraft = false
            )
            if(initialRecipe?.isDraft == true){
                val recipeDao = db.recipeDao()
                GlobalScope.launch(Dispatchers.IO) {
                    recipeDao.deleteAllRecipes()
                    withContext(Dispatchers.Main) {
                        onReload() // ✅ Trigger refresh after saving draft
                        dialog.dismiss()
                    }
                }
            }
            onConfirm(recipe)
            dialog.dismiss()
        }

        btnDraft.setOnClickListener {
            val recipe = RecipeD(
                id = 0 ,
                title = etTitle.text.toString(),
                description = etDescription.text.toString(),
                ingredients = etIngredients.text.toString().split(',').map { it.trim() },
                steps = etSteps.text.toString().split(',').map { it.trim() },
                category = spinnerCategory.selectedItem.toString(),
                imageUrl = etImageUrl.text.toString()
            )
            val recipeDao = db.recipeDao()
            GlobalScope.launch(Dispatchers.IO) {
                recipeDao.deleteAllRecipes()
                recipeDao.insertRecipe(recipe)

                withContext(Dispatchers.Main) {
                    onReload() // ✅ Trigger refresh after saving draft
                    dialog.dismiss()
                }
            }
        }



        val url = etImageUrl.text.toString().trim { it <= ' ' }
        if (!url.isEmpty()) {
            Glide.with(context)
                .load(url)
                .placeholder(R.drawable.ic_launcher_background)
                .into(imagePreview)
        }
        etImageUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val url = s.toString().trim()
                if (url.isNotEmpty()) {
                    Glide.with(context)
                        .load(url)
                        .placeholder(R.drawable.televisyen_png)
                        .error(R.drawable.ic_launcher_background)
                        .into(imagePreview)
                } else {
                    imagePreview.setImageResource(R.drawable.televisyen_png) // fallback or clear
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })



        btnClose.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

}
