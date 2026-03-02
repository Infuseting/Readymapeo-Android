package fr.infuseting.readymapeo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import fr.infuseting.readymapeo.data.repository.ClubRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ClubDetailViewModel(
    private val clubId: Int,
    private val clubRepository: ClubRepository
) : ViewModel() {

    var club by mutableStateOf<Club?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var updateSuccess by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            clubRepository.observeClub(clubId).collectLatest { c ->
                club = c
            }
        }
        refreshDetails()
    }

    fun refreshDetails() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                clubRepository.refreshClubDetails(clubId)
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun updateClub(request: UpdateClubRequest) {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            updateSuccess = false
            try {
                clubRepository.updateClub(clubId, request)
                updateSuccess = true
            } catch (e: Exception) {
                errorMessage = "Erreur de mise à jour : ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun resetUpdateSuccess() {
        updateSuccess = false
    }
}
