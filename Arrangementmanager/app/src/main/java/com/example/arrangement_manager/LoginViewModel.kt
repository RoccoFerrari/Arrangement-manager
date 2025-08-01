package com.example.arrangement_manager

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application, private val arrangementDao: ArrangementDAO) : AndroidViewModel(application) {
    private val _userSessionState = MutableStateFlow(UserSession())
    val userSessionState: StateFlow<UserSession> = _userSessionState.asStateFlow()

    fun loginOrRegister(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _userSessionState.value = _userSessionState.value.copy(isLoading = true, errorMessage = null)
            try {
                val userExists = arrangementDao.emailExist(email)
                if (!userExists) {
                    val newUser = User(email = email, password = password)
                    arrangementDao.insertUser(newUser)

                    // Aggiorna lo stato della sessione a loggato
                    _userSessionState.value = UserSession(email = newUser.email, isLoggedIn = true, isLoading = false)
                } else {
                    // L'utente esiste, procedi con la verifica della password per il login
                    val user = arrangementDao.getUserByEmail(email)
                    if (user != null && user.password == password) {
                        // Aggiorna lo stato della sessione a loggato
                        _userSessionState.value = UserSession(email = user.email, isLoggedIn = true, isLoading = false)

                    } else {
                        // Credenziali errate
                        _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Email o password errati.")
                    }
                }
            } catch (e: Exception) {
            // Errore generico
            _userSessionState.value = _userSessionState.value.copy(isLoading = false, errorMessage = "Errore: ${e.message}")
        }
        }
    }

    fun getAllUsers(): Flow<List<User>> {
        return arrangementDao.getAllUsers()
    }
}

class LoginViewModelFactory(private val application: Application, private val arrangementDao: ArrangementDAO) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Passa entrambi i parametri al costruttore di LoginViewModel
            return LoginViewModel(application, arrangementDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}