package fr.infuseting.readymapeo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.infuseting.readymapeo.data.model.Club
import fr.infuseting.readymapeo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClubListScreen(
    clubs: List<Club>,
    isLoading: Boolean,
    isOffline: Boolean,
    errorMessage: String?,
    onClubClick: (Int) -> Unit,
    onRefresh: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Mes Clubs",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    actionIconContentColor = OnPrimary
                ),
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                    IconButton(onClick = onLogoutClick) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Déconnexion")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── Bannière hors-ligne ──────────────────────────
            AnimatedVisibility(visible = isOffline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OfflineBanner)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠ Mode hors-ligne — données depuis le cache",
                        style = MaterialTheme.typography.bodySmall,
                        color = OfflineText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Contenu ──────────────────────────────────────
            when {
                isLoading && clubs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                errorMessage != null && clubs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Error
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = onRefresh) {
                                Text("Réessayer")
                            }
                        }
                    }
                }

                clubs.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏟",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aucun club trouvé",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(clubs) { club ->
                            ClubCard(
                                club = club,
                                onClick = { onClubClick(club.clubId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClubCard(
    club: Club,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icône colorée
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(listOf(Primary, Secondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = club.clubName.firstOrNull()?.uppercase() ?: "C",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = club.clubName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${club.clubCity} (${club.clubPostalCode})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge statut
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (club.isApproved) Success.copy(alpha = 0.15f) else Warning.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (club.isApproved) "Officiellement accepté" else "Non officiellement accepté",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (club.isApproved) Success else Warning,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
