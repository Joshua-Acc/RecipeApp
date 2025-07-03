package com.example.recipeapp.data

import com.example.recipeapp.PrefsHelper
import com.example.recipeapp.data.model.LoggedInUser
import java.io.IOException

/**
 * Class that handles authentication w/ login credentials and retrieves user information.
 */
class LoginDataSource {

    fun login(username: String, password: String): Result<LoggedInUser> {
        try {
            // TODO: handle loggedInUser authentication
            if (PrefsHelper.isLoggedIn != false) {
                val user = LoggedInUser(PrefsHelper.uid.toString(), PrefsHelper.username.toString())
                return Result.Success(user)

            }else{
                val user = LoggedInUser("", "")
                return Result.Success(user)

            }
        } catch (e: Throwable) {
            return Result.Error(IOException("Error logging in", e))
        }
    }

    fun logout() {
        // TODO: revoke authentication
    }
}