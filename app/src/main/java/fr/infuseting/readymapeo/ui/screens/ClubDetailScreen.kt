package fr.infuseting.readymapeo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.data.model.UpdateClubRequest
import fr.infuseting.readymapeo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubDetailScreen(
    club: Club?,
    isLoading: Boolean,
    errorMessage: String?,
    updateSuccess: Boolean,
    isOffline: Boolean,
    onNavigateBack: () -> Unit,
    onManageMembersClick: (Int) -> Unit,
    onShareInviteLink: (Int, String) -> Unit,
    onUpdateClub: (UpdateClubRequest) -> Unit,
    onResetUpdateSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    // Champs d'édition
    var editName by remember(club) { mutableStateOf(club?.clubName ?: "") }
    var editStreet by remember(club) { mutableStateOf(club?.clubStreet ?: "") }
    var editCity by remember(club) { mutableStateOf(club?.clubCity ?: "") }
    var editPostalCode by remember(club) { mutableStateOf(club?.clubPostalCode ?: "") }
    var editDescription by remember(club) { mutableStateOf(club?.description ?: "") }
    var editFfsoId by remember(club) { mutableStateOf(club?.ffsoId ?: "") }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(updateSuccess) {
        if (updateSuccess) {
            val suffix = if (isOffline) " (sera synchronisé)" else ""
            snackbarHostState.showSnackbar("Club mis à jour !$suffix")
            isEditing = false
            onResetUpdateSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    LaunchedEffect(club?.isApproved) {
        if (club?.isApproved != true) {
            isEditing = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(club?.clubName ?: "Détails du club") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary,
                    actionIconContentColor = OnPrimary
                ),
                actions = {
                    // N'afficher le bouton "Modifier" que si le club existe ET qu'il est approuvé
                    if (club != null && club.isApproved) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(Icons.Default.Edit, contentDescription = "Modifier")
                        }
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        when {
            isLoading && club == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Primary)
                }
            }

            club == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Club introuvable", color = Error)
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Mode lecture ──────────────────────────
                    AnimatedVisibility(visible = !isEditing) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Header card
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    DetailRow("Nom", club.clubName)
                                    DetailRow("Adresse", club.clubStreet)
                                    DetailRow("Ville", "${club.clubCity} (${club.clubPostalCode})")
                                    club.ffsoId?.let { DetailRow("FFSO ID", it) }
                                    club.description?.let { DetailRow("Description", it) }
                                    DetailRow(
                                        "Statut",
                                        if (club.isApproved) {
                                            "Officiellement accepté"
                                        } else {
                                            "Non officiellement accepté"
                                        }
                                    )
                                }
                            }

                            if (!club.isApproved) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Warning.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Ce club n'est pas officiellement accepté pour le moment.",
                                        color = Warning,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }


                            if (club.isApproved) {
                                Button(
                                    onClick = { onManageMembersClick(club.clubId) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Secondary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Gérer les membres",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onShareInviteLink(club.clubId, club.clubName) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Inviter par lien", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // ── Mode édition ─────────────────────────
                    AnimatedVisibility(visible = isEditing) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Modifier le club",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                OutlinedTextField(
                                    value = editName,
                                    onValueChange = { editName = it },
                                    label = { Text("Nom du club") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editStreet,
                                    onValueChange = { editStreet = it },
                                    label = { Text("Adresse") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editCity,
                                        onValueChange = { editCity = it },
                                        label = { Text("Ville") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(2f)
                                    )
                                    OutlinedTextField(
                                        value = editPostalCode,
                                        onValueChange = { editPostalCode = it },
                                        label = { Text("CP") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                OutlinedTextField(
                                    value = editFfsoId,
                                    onValueChange = { editFfsoId = it },
                                    label = { Text("FFSO ID") },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = editDescription,
                                    onValueChange = { editDescription = it },
                                    label = { Text("Description") },
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 3,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { isEditing = false },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Annuler")
                                    }
                                    Button(
                                        onClick = {
                                            onUpdateClub(
                                                UpdateClubRequest(
                                                    clubName = editName.ifBlank { null },
                                                    clubStreet = editStreet.ifBlank { null },
                                                    clubCity = editCity.ifBlank { null },
                                                    clubPostalCode = editPostalCode.ifBlank { null },
                                                    ffsoId = editFfsoId.ifBlank { null },
                                                    description = editDescription.ifBlank { null }
                                                )
                                            )
                                        },
                                        enabled = !isLoading,
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = OnPrimary,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text("Sauvegarder", fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
