package com.example.recipeapp.ui.home

import android.animation.Animator
import android.animation.AnimatorInflater
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.recipeapp.FirebaseApiManager
import com.example.recipeapp.R
import com.example.recipeapp.data.model.User
import com.example.recipeapp.databinding.FragmentHomeBinding
import com.example.recipeapp.ui.UiHelper.applyTextAndBackgroundStyle
import com.example.recipeapp.ui.login.LoggedInUserView
import com.example.recipeapp.ui.login.LoginViewModel
import com.example.recipeapp.ui.login.LoginViewModelFactory
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var usernameText : EditText
    private lateinit var emailText : EditText
    private lateinit var saveButton : Button
    private lateinit var cancelButton : Button
    private lateinit var loginViewModel: LoginViewModel
    private var waveAnimator: Animator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        val root: View = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginViewModel = ViewModelProvider(requireActivity(), LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        val wave = view.findViewById<ImageView>(R.id.waveTop)
        val wave2 = view.findViewById<ImageView>(R.id.waveTop2)
        waveAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.wave_shift).apply {
            setTarget(wave)
            setTarget(wave2)
            start()
        }

        initializeUiElement()
        loadUserProfile()
        performAction()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)

        menu.clear()
        inflater.inflate(R.menu.home_main, menu)

    }



    private fun performAction(){
        saveButton.setOnClickListener{
            saveUserProfile()
        }
        cancelButton.setOnClickListener{
            findNavController().navigate(R.id.action_navigation_home_to_navigation_menu)
        }
    }
    private fun insertNewRecipe() {
        // Example: Show a simple message or navigate to a "NewRecipeFragment"
        Toast.makeText(requireContext(), "Insert button clicked!", Toast.LENGTH_SHORT).show()

        // TODO: Navigate to a form or dialog for entering new recipe details
    }


    private fun initializeUiElement(){
        usernameText = binding.txtUser
        emailText = binding.txtEmail
        saveButton = binding.btnSaveProfileEdit
        cancelButton = binding.btnCancelProfileEdit

        loginViewModel.loginFormState.observe(
            viewLifecycleOwner,
            Observer { loginFormState ->
                if (loginFormState == null) {
                    return@Observer
                }
                saveButton.isEnabled = loginFormState.isDataValid
                loginFormState.usernameError?.let {
                    usernameText.error = getString(it)
                }
            })
        val afterTextChangedListener = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                // ignore
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                // ignore
            }

            override fun afterTextChanged(s: Editable) {
                loginViewModel.profileDataChanged(
                    usernameText.text.toString()
                )
            }
        }
        usernameText.addTextChangedListener(afterTextChangedListener)
    }

    private fun saveUserProfile(){
        val uid = auth.currentUser?.uid ?: return
        val dbPath = "users/$uid"
        val user = User(
            name = usernameText.text.toString(),
            email = emailText.text.toString(),
            uid = "U${uid.takeLast(10)}" // or your custom UID logic
        )
        FirebaseApiManager().saveDataToFirebase(
            path = "users/$uid",
            data = user,
            onSuccess = {
                Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_navigation_home_to_navigation_menu)
            },
            onError = { error ->
                Log.e("Firebase", "Failed to save profile", error.toException())
            }
        )
    }

    private fun loadUserProfile(){
        val uid = auth.currentUser?.uid ?: return
        val dbPath = "users/$uid"

        FirebaseApiManager().loadDataFromFirebase<User>(
            path = dbPath,
            onSuccess = { profile ->
                profile?.let {
                    usernameText.setText(it.name ?: "")
                    emailText.setText(it.email ?: "")
                    emailText.isEnabled = false

                    emailText.applyTextAndBackgroundStyle(
                        textColorHex = "#CC000000", // semi-transparent black
                        backgroundAlpha = 0.5f      // 50% background opacity
                    )
                }
            },
            onError = { error ->
                Log.e("Firebase", "Error loading profile", error.toException())
            }
        )
    }

    override fun onDestroyView() {
        waveAnimator?.cancel()
        super.onDestroyView()
        _binding = null
    }
}