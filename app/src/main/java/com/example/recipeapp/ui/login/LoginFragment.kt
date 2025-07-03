package com.example.recipeapp.ui.login

import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.Intent
import android.graphics.Bitmap

import android.graphics.Color
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.compose.runtime.remember
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import com.example.recipeapp.FirebaseApiManager
import com.example.recipeapp.MainActivity
import com.example.recipeapp.PrefsHelper
import com.example.recipeapp.databinding.FragmentLoginBinding

import com.example.recipeapp.R
import com.example.recipeapp.data.model.ActivationKey
import com.example.recipeapp.data.model.User
import com.example.recipeapp.ui.UiHelper.applyTextAndBackgroundStyle
import com.example.recipeapp.ui.UiHelper.disableTemporarily
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import java.util.Calendar
import java.util.TimeZone


class LoginFragment : Fragment() {

    private lateinit var loginViewModel: LoginViewModel
    private var _binding: FragmentLoginBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    var btnDisabled : Boolean = true
    private var waveAnimator: Animator? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        auth = FirebaseAuth.getInstance()


        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loginViewModel = ViewModelProvider(this, LoginViewModelFactory())
            .get(LoginViewModel::class.java)

        val usernameEditText = binding.username
        val passwordEditText = binding.password
        val loginButton = binding.btnLogin
        val registerButton = binding.btnRegister
        val loadingProgressBar = binding.loading
        val activateButton = binding.btnActivation
        val greetingText = binding.greetingText


        val calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Kuala_Lumpur"))
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

       /* val greeting = when (hour) {
            in 5..11 -> "Good morning!"
            in 12..17 -> "Good afternoon!"
            in 18..21 -> "Good evening!"
            else -> "Welcome!"
        } */

        val wave = view.findViewById<ImageView>(R.id.waveTop3)
        val wave2 = view.findViewById<ImageView>(R.id.waveTop4)

        waveAnimator = AnimatorInflater.loadAnimator(requireContext(), R.animator.wave_shift).apply {
            setTarget(wave2)
            setTarget(wave)
            start()
        }


        loginButton.visibility = View.GONE

            verifyActivationKey()

            loginViewModel.loginFormState.observe(
                viewLifecycleOwner,
                Observer { loginFormState ->
                    if (loginFormState == null) {
                        return@Observer
                    }
                    loginButton.isEnabled = loginFormState.isDataValid

                    loginFormState.usernameError?.let {
                        usernameEditText.error = getString(it)
                    }
                    loginFormState.passwordError?.let {
                        passwordEditText.error = getString(it)
                    }
                })

            loginViewModel.loginResult.observe(
                viewLifecycleOwner,
                Observer { loginResult ->
                    loginResult ?: return@Observer
                    loadingProgressBar.visibility = View.GONE
                    loginResult.error?.let {
                        showLoginFailed(it)
                    }
                    loginResult.success?.let {
                        loginUser(it)
                    }
                })

            val afterTextChangedListener = object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // ignore
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    // ignore
                }

                override fun afterTextChanged(s: Editable) {
                    loginViewModel.loginDataChanged(
                        usernameEditText.text.toString(),
                        passwordEditText.text.toString()
                    )
                }
            }
            usernameEditText.addTextChangedListener(afterTextChangedListener)
            passwordEditText.addTextChangedListener(afterTextChangedListener)
            passwordEditText.setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    loginViewModel.login(
                        usernameEditText.text.toString(),
                        passwordEditText.text.toString()
                    )
                }
                false
            }


            loginButton.setOnClickListener {
                loginButton.disableTemporarily()

                loadingProgressBar.visibility = View.VISIBLE
                loginViewModel.login(
                    usernameEditText.text.toString(),
                    passwordEditText.text.toString()
                )
            }
            registerButton.setOnClickListener {
                loginButton.disableTemporarily()

                loadingProgressBar.visibility = View.VISIBLE
                findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
            }

            activateButton.setOnClickListener {
                findNavController().navigate(R.id.action_loginFragment_to_qrScannerFragment)
            }

    }
    private fun verifyActivationKey() {
        try {
        val dbRef = FirebaseDatabase.getInstance().getReference("activationKey")

        val genericTypeIndicator = object : GenericTypeIndicator<Map<String, Map<String, String>>>() {}

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val keyMap = snapshot.getValue(genericTypeIndicator)
                val localKey = PrefsHelper.activateKey
                var matchFound = false

                keyMap?.forEach { (parentKey, nestedMap) ->
                    val firebaseKey = nestedMap["key"]
                    Log.d("activationKey-trace", "Checking [$parentKey.key]: $firebaseKey vs local: $localKey")
                    if (firebaseKey == localKey) matchFound = true
                }
                binding.btnLogin.isVisible = matchFound

                btnDisabled = !matchFound ?: true

                if (!matchFound) {
                    Log.w("activationKey", "No match for local key: $localKey")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error verifying activation key", error.toException())
                binding.btnLogin.isEnabled = false
                btnDisabled = true
            }
        })
        }catch (ex:Exception){

        }
    }


    private fun loginUser(loggedInUserView: LoggedInUserView){
        val usernameEditText = binding.username
        val passwordEditText = binding.password
        auth.signInWithEmailAndPassword( usernameEditText.text.toString(), passwordEditText.text.toString()).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser!!
                val ref = FirebaseDatabase.getInstance().getReference("users")

                ref.child(firebaseUser.uid)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            val uid = snapshot.child("uid").value
                            val name = snapshot.child("name").value
                            val email = snapshot.child("email").value

                            PrefsHelper.uid = uid.toString()
                            PrefsHelper.username = name.toString()
                            PrefsHelper.email = email.toString()
                            PrefsHelper.isLoggedIn = true

                            Log.d("loginDetail:", uid.toString() +","+ name.toString()+ "," + email.toString() )
                            val loggedInUserView1 = LoggedInUserView(displayName = name.toString())
                            updateUiWithUser(loggedInUserView1)


                            val intent = Intent( context, MainActivity::class.java )
                            startActivity(intent)
                        }
                        override fun onCancelled(error: DatabaseError) {
                            TODO("Not yet implemented")
                        }
                    })

            }
        }
    }

    private fun updateUiWithUser(model: LoggedInUserView) {
        val welcome = getString(R.string.welcome) +  model.displayName
        // TODO : initiate successful logged in experience
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, welcome, Toast.LENGTH_LONG).show()
    }

    private fun showLoginFailed(@StringRes errorString: Int) {
        val appContext = context?.applicationContext ?: return
        Toast.makeText(appContext, errorString, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        waveAnimator?.cancel()
        super.onDestroyView()
        _binding = null
    }
}