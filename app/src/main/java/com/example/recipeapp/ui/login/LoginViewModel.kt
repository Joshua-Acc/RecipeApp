package com.example.recipeapp.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.util.Patterns
import com.example.recipeapp.data.LoginRepository
import com.example.recipeapp.data.Result

import com.example.recipeapp.R

class LoginViewModel(private val loginRepository: LoginRepository, var formType: String? = "") : ViewModel() {

    private val _loginForm = MutableLiveData<LoginFormState>()
    val loginFormState: LiveData<LoginFormState> = _loginForm

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun login(username: String, password: String) {
        // can be launched in a separate asynchronous job
        val result = loginRepository.login(username, password)

        if (result is Result.Success) {
            _loginResult.value =
                LoginResult(success = LoggedInUserView(displayName = result.data.displayName))
        } else {
            _loginResult.value = LoginResult(error = R.string.login_failed)
        }
    }

    fun loginDataChanged(username: String, password: String, confirmPassword: String? = null) {
        when {
            !isUserNameValid(username) -> {
                _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
            }
            !isPasswordValid(password) -> {
                _loginForm.value = LoginFormState(passwordError = R.string.invalid_password)
            }
            formType.equals("REGISTER", ignoreCase = true) && confirmPassword != null && password != confirmPassword -> {
                _loginForm.value = LoginFormState(confirmPasswordError = R.string.passwords_do_not_match)
            }
            else -> {
                _loginForm.value = LoginFormState(isDataValid = true)
            }
        }
    }

    fun profileDataChanged(username: String) {
        when {
            !isUserNameValid(username) -> {
                _loginForm.value = LoginFormState(usernameError = R.string.invalid_username)
            }else -> {
                _loginForm.value = LoginFormState(isDataValid = true)
            }
        }
    }


    // A placeholder username validation check
    private fun isUserNameValid(username: String): Boolean {
        return if (username.contains("@")) {
            Patterns.EMAIL_ADDRESS.matcher(username).matches()
        } else {
            username.isNotBlank()
        }
    }

    // A placeholder password validation check
    private fun isPasswordValid(password: String): Boolean {
        return password.length > 5
    }
}