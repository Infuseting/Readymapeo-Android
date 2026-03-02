package fr.infuseting.readymapeo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.infuseting.readymapeo.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    var isLoading by mutableStateOf(false)
        private set

    var loginError by mutableStateOf<String?>(null)
        private set

    var loginSuccess by mutableStateOf(false)
        private set

    fun login(email: String, password: String) {
        viewModelScope.launch {
            isLoading = true
            loginError = null
            loginSuccess = false
            try {
                authRepository.login(email, password)
                loginSuccess = true
            } catch (e: Exception) {
                loginError = e.message ?: "Erreur de connexion"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetState() {
        loginError = null
        loginSuccess = false
    }
}
