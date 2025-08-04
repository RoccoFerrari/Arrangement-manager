package com.example.arrangement_manager

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
import com.example.arrangement_manager.RetrofitClient.apiService
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val _userSessionState = MutableStateFlow(UserSession())
    val userSessionState: StateFlow<UserSession> = _userSessionState.asStateFlow()

    fun loginOrRegister(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Reimposta lo stato all'inizio di ogni tentativo
            _userSessionState.value =
                _userSessionState.value.copy(isLoading = true, errorMessage = null)
            val user = User(email = email, password = password)
            try {
                // Prova a eseguire il login
                val loginResponse = apiService.loginUser(user)

                if (loginResponse.isSuccessful) {
                    val loginBody = loginResponse.body()
                    val loggedInUser = loginBody?.user
                    if (loggedInUser != null) {
                        _userSessionState.value = UserSession(
                            email = loggedInUser.email,
                            isLoggedIn = true,
                            isLoading = false
                        )
                    } else {
                        _userSessionState.value = _userSessionState.value.copy(
                            isLoading = false,
                            errorMessage = "Risposta del server non valida."
                        )
                    }
                } else {
                    when (loginResponse.code()) {
                        // Credenziali non valide (l'email esiste ma la password è sbagliata)
                        401 -> {
                            _userSessionState.value = _userSessionState.value.copy(
                                isLoading = false,
                                errorMessage = "Email o password errati."
                            )
                        }
                        // Utente non trovato (l'email non esiste), prova a registrarlo
                        404 -> {
                            tryRegisterUser(user)
                        }
                        // Errore generico
                        else -> {
                            _userSessionState.value = _userSessionState.value.copy(
                                isLoading = false,
                                errorMessage = "Errore di login: ${loginResponse.message()}"
                            )
                        }
                    }
                }
            } catch (e: HttpException) {
                // Gestione dell'eccezione HTTP: Not Found (404)
                if (e.code() == 404) {
                    tryRegisterUser(user)
                } else {
                    _userSessionState.value = _userSessionState.value.copy(
                        isLoading = false,
                        errorMessage = "Errore di login: ${e.message}"
                    )
                }
            } catch (e: IOException) {
                // Errore di rete (es. disconnessione)
                _userSessionState.value = _userSessionState.value.copy(
                    isLoading = false,
                    errorMessage = "Errore di connessione. Controlla la tua rete."
                )
            } catch (e: Exception) {
                // Errore generico inaspettato
                _userSessionState.value = _userSessionState.value.copy(
                    isLoading = false,
                    errorMessage = "Errore inaspettato: ${e.message}"
                )
            }
        }
    }

    private fun tryRegisterUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            _userSessionState.value = _userSessionState.value.copy(isLoading = true, errorMessage = null)
            try {
                // Registrazione
                val registerResponse = apiService.registerUser(user)

                if (registerResponse.isSuccessful) {
                    val registeredUser = registerResponse.body()
                    if (registeredUser != null) {
                        _userSessionState.value = UserSession(email = registeredUser.email, isLoggedIn = true, isLoading = false)
                    } else {
                        _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Risposta di registrazione non valida.")
                    }
                } else {
                    // Gestione dell'errore di registrazione
                    val errorMessage = when (registerResponse.code()) {
                        409 -> "L'email esiste già."
                        else -> "Errore di registrazione: ${registerResponse.message()}"
                    }
                    _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = errorMessage)
                }
            } catch (e: Exception) {
                _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Errore di rete o server: ${e.message}")
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