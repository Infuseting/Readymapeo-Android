package fr.infuseting.readymapeo.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.infuseting.readymapeo.data.model.User
import fr.infuseting.readymapeo.data.repository.ClubRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MemberManagementViewModel(
    private val clubId: Int,
    private val clubRepository: ClubRepository
) : ViewModel() {

    var approvedMembers by mutableStateOf<List<User>>(emptyList())
        private set

    var pendingMembers by mutableStateOf<List<User>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var actionSuccess by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            clubRepository.observeApprovedMembers(clubId).collectLatest {
                approvedMembers = it
            }
        }
        viewModelScope.launch {
            clubRepository.observePendingMembers(clubId).collectLatest {
                pendingMembers = it
            }
        }
        refreshMembers()
    }

    fun refreshMembers() {
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

    fun approveMember(userId: Int) {
        viewModelScope.launch {
            try {
                clubRepository.approveMember(clubId, userId)
                actionSuccess = "Membre approuvé"
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.message}"
            }
        }
    }

    fun rejectMember(userId: Int) {
        viewModelScope.launch {
            try {
                clubRepository.rejectMember(clubId, userId)
                actionSuccess = "Membre rejeté"
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.message}"
            }
        }
    }

    fun removeMember(userId: Int, currentUserId: Int? = null) {
        if (currentUserId != null && currentUserId == userId) {
            errorMessage = "Impossible de vous retirer vous-même"
            return
        }

        viewModelScope.launch {
            try {
                clubRepository.removeMember(clubId, userId)
                actionSuccess = "Membre retiré"
            } catch (e: Exception) {
                errorMessage = "Erreur : ${e.message}"
            }
        }
    }

    fun resetActionSuccess() {
        actionSuccess = null
    }
}
