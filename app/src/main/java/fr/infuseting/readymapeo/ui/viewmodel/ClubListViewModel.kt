package fr.infuseting.readymapeo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.repository.ClubRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClubListViewModel(
    private val clubRepository: ClubRepository
) : ViewModel() {

    var clubs by mutableStateOf<List<Club>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        // Observer les clubs depuis Room
        viewModelScope.launch {
            clubRepository.observeClubs().collectLatest { clubList ->
                clubs = clubList
            }
        }
        // Refresh initial
        refreshClubs()
    }

    fun refreshClubs() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                clubRepository.refreshClubs()
            } catch (e: Exception) {
                errorMessage = "Erreur de chargement : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
}
