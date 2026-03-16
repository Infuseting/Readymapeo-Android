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
import org.json.JSONObject

class ClubDetailViewModel(
    private val clubId: Int,
    private val clubRepository: ClubRepository
) : ViewModel() {

    var club by mutableStateOf<Club?>(null)
        private set

    var approvedMembers by mutableStateOf<List<fr.infuseting.readymapeo.data.model.User>>(emptyList())
        private set

    var pendingMembers by mutableStateOf<List<fr.infuseting.readymapeo.data.model.User>>(emptyList())
        private set

    var isManager by mutableStateOf<Boolean?>(null)
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
        // Observer les membres pour permettre l'affichage direct
        viewModelScope.launch {
            clubRepository.observeApprovedMembers(clubId).collectLatest { list ->
                approvedMembers = list
            }
        }
        viewModelScope.launch {
            clubRepository.observePendingMembers(clubId).collectLatest { list ->
                pendingMembers = list
            }
        }
        // Vérifier si on est manager (cela mettra aussi à jour le cache local via la méthode)
        viewModelScope.launch {
            isManager = try {
                clubRepository.isUserManagerOf(clubId)
            } catch (_: Exception) {
                false
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
                val response = clubRepository.updateClub(clubId, request)

                if (response == null) {
                    // Hors-ligne : la mise à jour locale a été faite et l'action est en file d'attente
                    updateSuccess = true
                    return@launch
                }

                // Parse server response JSON and check for status == "success"
                val json = JSONObject(response)
                val status = when {
                    json.has("status") -> json.optString("status", "")
                    json.has("data") && json.optJSONObject("data")?.has("status") == true -> json.optJSONObject("data")?.optString("status", "")
                    else -> ""
                }

                if (status.equals("success", ignoreCase = true)) {
                    updateSuccess = true
                } else {
                    // Try to extract a message
                    val message = when {
                        json.has("message") -> json.optString("message", "Erreur inconnue")
                        json.has("data") && json.optJSONObject("data")?.has("message") == true -> json.optJSONObject("data")?.optString("message", "Erreur inconnue")
                        else -> "Erreur inconnue"
                    }
                    errorMessage = message
                }

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
