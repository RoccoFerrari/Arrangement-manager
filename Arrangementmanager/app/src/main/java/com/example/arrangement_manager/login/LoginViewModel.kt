package com.example.arrangement_manager.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.arrangement_manager.retrofit.RetrofitClient.apiService
import com.example.arrangement_manager.retrofit.User
import com.example.arrangement_manager.socket_handler.SocketHandler
import retrofit2.HttpException
import java.io.IOException

/**
 * ViewModel responsible for user login and registration logic.
 *
 * This ViewModel handles all network calls to authenticate users and manage their session state.
 * It exposes a [StateFlow] to the UI to provide real-time updates on the login process,
 * including loading state, error messages, and successful login status.
 *
 * @param application The application context.
 */
class LoginViewModel(application: Application) : AndroidViewModel(application) {
    // StateFlow to hold the user session state, including loading, error, and login status
    private val _userSessionState = MutableStateFlow(UserSession())
    val userSessionState: StateFlow<UserSession> = _userSessionState.asStateFlow()

    /**
     * Attempts to log in or register a user with the provided email and password.
     *
     * First, it tries to log in. If the user is not found (404 error), it attempts to register the user.
     * It handles various network and server errors and updates the UI state accordingly.
     *
     * @param email The user's email address.
     * @param password The user's password.
     */
    fun loginOrRegister(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Reset state at the start of each attempt
            _userSessionState.value =
                _userSessionState.value.copy(isLoading = true, errorMessage = null)
            val user = User(email = email, password = password)
            try {
                // Try logging in
                val loginResponse = apiService.loginUser(user)

                if (loginResponse.isSuccessful) {
                    val loginBody = loginResponse.body()
                    val loggedInUser = loginBody?.user
                    if (loggedInUser != null) {
                        SocketHandler.setSocket(loggedInUser.email)
                        SocketHandler.establishConnection()

                        _userSessionState.value = UserSession(
                            email = loggedInUser.email,
                            isLoggedIn = true,
                            isLoading = false
                        )
                        val sessionManager = SessionManager(getApplication())
                        sessionManager.saveUserSession(email)
                    } else {
                        _userSessionState.value = _userSessionState.value.copy(
                            isLoading = false,
                            errorMessage = "Invalid server response."
                        )
                    }
                } else {
                    when (loginResponse.code()) {
                        // Credentials not valid (email exists but wrong password)
                        401 -> {
                            _userSessionState.value = _userSessionState.value.copy(
                                isLoading = false,
                                errorMessage = "Email or password wrong."
                            )
                        }
                        // User not found (email address does not exist), try registering
                        404 -> {
                            tryRegisterUser(user)
                        }
                        // Generic error
                        else -> {
                            _userSessionState.value = _userSessionState.value.copy(
                                isLoading = false,
                                errorMessage = "Login error: ${loginResponse.message()}"
                            )
                        }
                    }
                }
            } catch (e: HttpException) {
                // HTTP Exception Handling: Not Found (404)
                if (e.code() == 404) {
                    tryRegisterUser(user)
                } else {
                    _userSessionState.value = _userSessionState.value.copy(
                        isLoading = false,
                        errorMessage = "Login error: ${e.message}"
                    )
                }
            } catch (e: IOException) {
                // Network error (e.g. disconnection)
                _userSessionState.value = _userSessionState.value.copy(
                    isLoading = false,
                    errorMessage = "${e.message}")
            } catch (e: Exception) {
                // Unexpected generic error
                _userSessionState.value = _userSessionState.value.copy(
                    isLoading = false,
                    errorMessage = "Unexpected error: ${e.message}"
                )
            }
        }
    }

    /**
     * Attempts to register a new user with the provided details.
     *
     * This method is typically called after a login attempt fails with a 404 (Not Found) error.
     * It handles successful registration and potential errors like a duplicate email (409).
     *
     * @param user The [User] object containing the email and password for registration.
     */
    private fun tryRegisterUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            _userSessionState.value = _userSessionState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Registration
                val registerResponse = apiService.registerUser(user)

                if (registerResponse.isSuccessful) {
                    val registeredUser = registerResponse.body()
                    if (registeredUser != null) {
                        SocketHandler.setSocket(registeredUser.email)
                        SocketHandler.establishConnection()

                        _userSessionState.value = UserSession(email = registeredUser.email, isLoggedIn = true, isLoading = false)
                    } else {
                        _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Invalid registration response.")
                    }
                } else {
                    // Registration error handling
                    val errorMessage = when (registerResponse.code()) {
                        409 -> "The email already exists."
                        else -> "Registration error: ${registerResponse.message()}"
                    }
                    _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = errorMessage)
                }
            } catch (e: Exception) {
                _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Network or server error: ${e.message}")
            }
        }
    }
}

class LoginViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}