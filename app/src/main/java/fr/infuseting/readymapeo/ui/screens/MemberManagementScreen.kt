package fr.infuseting.readymapeo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.infuseting.readymapeo.data.model.User
import fr.infuseting.readymapeo.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberManagementScreen(
    approvedMembers: List<User>,
    pendingMembers: List<User>,
    isLoading: Boolean,
    errorMessage: String?,
    actionSuccess: String?,
    isOffline: Boolean,
    onNavigateBack: () -> Unit,
    onApproveMember: (Int) -> Unit,
    onRejectMember: (Int) -> Unit,
    onRemoveMember: (Int) -> Unit,
    onResetActionSuccess: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Membres (${approvedMembers.size})", "En attente (${pendingMembers.size})")

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionSuccess) {
        actionSuccess?.let {
            val suffix = if (isOffline) " (sera synchronisé)" else ""
            snackbarHostState.showSnackbar("$it$suffix")
            onResetActionSuccess()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Gestion des membres") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Primary,
                    titleContentColor = OnPrimary,
                    navigationIconContentColor = OnPrimary,
                    actionIconContentColor = OnPrimary
                )
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
                        text = "⚠ Mode hors-ligne",
                        style = MaterialTheme.typography.bodySmall,
                        color = OfflineText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Onglets ──────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // ── Contenu ──────────────────────────────────────
            when {
                isLoading && approvedMembers.isEmpty() && pendingMembers.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                }

                else -> {
                    val currentList = if (selectedTab == 0) approvedMembers else pendingMembers
                    val isPendingTab = selectedTab == 1

                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isPendingTab) "Aucune demande en attente" else "Aucun membre",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(currentList) { user ->
                                MemberCard(
                                    user = user,
                                    isPending = isPendingTab,
                                    onApprove = { onApproveMember(user.id) },
                                    onReject = { onRejectMember(user.id) },
                                    onRemove = { onRemoveMember(user.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberCard(
    user: User,
    isPending: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Primary.copy(alpha = 0.7f),
                                Secondary.copy(alpha = 0.7f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.firstOrNull()?.uppercase() ?: "?",
                    color = OnPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isPending) {
                    Text(
                        text = "Non officiellement accepté",
                        style = MaterialTheme.typography.labelMedium,
                        color = Warning,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // Actions
            if (isPending) {
                IconButton(
                    onClick = onApprove,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Success)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Approuver")
                }
                IconButton(
                    onClick = onReject,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Error)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Rejeter")
                }
            } else {
                IconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = Error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Retirer")
                }
            }
        }
    }
}
