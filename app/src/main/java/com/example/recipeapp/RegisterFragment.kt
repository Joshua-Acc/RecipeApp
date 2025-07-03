package com.example.recipeapp

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.recipeapp.data.model.User
import com.example.recipeapp.databinding.FragmentRegisterBinding
import com.example.recipeapp.ui.login.LoginViewModel
import com.example.recipeapp.ui.login.LoginViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [RegisterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegisterFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth: FirebaseAuth
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private lateinit var loginViewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        loginViewModel.formType = "REGISTER"

        val emailText = binding.txtEmail
        val passwordEditText = binding.txtPassword
        val confirmPasswordEditText = binding.txtConfirmPassword
        val submitButton = binding.btnSubmit
        val backButton = binding.btnBack

        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                loginViewModel.loginDataChanged(
                    emailText.text.toString(),
                    passwordEditText.text.toString(),
                    confirmPasswordEditText.text.toString()
                )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        emailText.addTextChangedListener(textWatcher)
        passwordEditText.addTextChangedListener(textWatcher)
        confirmPasswordEditText.addTextChangedListener(textWatcher)

        loginViewModel.loginFormState.observe(viewLifecycleOwner, Observer { loginFormState ->
            loginFormState ?: return@Observer
            submitButton.isEnabled = loginFormState.isDataValid
            loginFormState.usernameError?.let {
                emailText.error = getString(it)
            }
            loginFormState.passwordError?.let {
                passwordEditText.error = getString(it)
            }
            loginFormState.confirmPasswordError?.let {
                confirmPasswordEditText.error = getString(it)
            }
        })

        backButton.setOnClickListener{
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        submitButton.setOnClickListener {
            val email = binding.txtEmail.text.toString().trim()
            val password = binding.txtPassword.text.toString().trim()
            val confirmPassword = binding.txtConfirmPassword.text.toString().trim()

            if (password != confirmPassword) {
                binding.txtConfirmPassword.error = getString(R.string.passwords_do_not_match)
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(requireActivity()) { task ->
                    if (task.isSuccessful) {
                        val usersRef = FirebaseDatabase.getInstance().getReference("users")

                        usersRef.orderByChild("uid").limitToLast(1).get()

                            .addOnSuccessListener { snapshot ->
                                var newIdNumber = 1L

                                // If there are existing users, find the last custom UID
                                val lastUid = snapshot.children.firstOrNull()?.child("uid")?.value as? String
                                if (lastUid != null && lastUid.startsWith("U")) {
                                    val numericPart = lastUid.substring(1).toLongOrNull()
                                    if (numericPart != null) {
                                        newIdNumber = numericPart + 1
                                    }
                                }
                                val customUserId = "U" + String.format("%010d", newIdNumber)
                                val firebaseUid = auth.currentUser?.uid ?: return@addOnSuccessListener
                                val user = User(
                                    uid = customUserId,
                                    name = binding.txtName.text.toString().trim(),
                                    email = binding.txtEmail.text.toString().trim()
                                )

                                usersRef.child(firebaseUid).setValue(user)
                               // usersRef.child(customUserId).setValue(user)
                                    .addOnSuccessListener {
                                        Toast.makeText(context, "User saved with ID: $customUserId", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(context, "Database error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }

                                findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
                            }
                            .addOnFailureListener { e ->
                                Log.d("RegisteredError", "Failed to read last user ID: ${e.message}")
                                Toast.makeText(context, "Failed to read last user ID: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Auth error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RegisterFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RegisterFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}