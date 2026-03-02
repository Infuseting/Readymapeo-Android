package fr.infuseting.readymapeo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import fr.infuseting.readymapeo.data.local.AppDatabase
import fr.infuseting.readymapeo.data.local.TokenManager
import fr.infuseting.readymapeo.data.remote.ApiClient
import fr.infuseting.readymapeo.data.remote.AuthApiService
import fr.infuseting.readymapeo.data.remote.ClubApiService
import fr.infuseting.readymapeo.data.repository.AuthRepository
import fr.infuseting.readymapeo.data.repository.ClubRepository
import fr.infuseting.readymapeo.navigation.AppRoutes
import fr.infuseting.readymapeo.sync.ConnectivityObserver
import fr.infuseting.readymapeo.sync.SyncManager
import fr.infuseting.readymapeo.ui.screens.*
import fr.infuseting.readymapeo.ui.theme.ReadymapeoTheme
import fr.infuseting.readymapeo.ui.viewmodel.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ReadymapeoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReadymapeoApp()
                }
            }
        }
    }
}

@Composable
fun ReadymapeoApp() {
    val context = LocalContext.current
    val navController = rememberNavController()

    // ── Initialisation des dépendances ───────────────────────
    val tokenManager = remember { TokenManager(context) }
    val apiClient = remember { ApiClient(tokenManager) }
    val authApiService = remember { AuthApiService(apiClient) }
    val clubApiService = remember { ClubApiService(apiClient) }

    val database = remember { AppDatabase.getInstance(context) }
    val clubDao = remember { database.clubDao() }
    val pendingActionDao = remember { database.pendingActionDao() }

    val connectivityObserver = remember { ConnectivityObserver(context) }
    val syncManager = remember { SyncManager(pendingActionDao, clubApiService) }

    val authRepository = remember { AuthRepository(authApiService, tokenManager) }
    val clubRepository = remember {
        ClubRepository(clubDao, pendingActionDao, clubApiService, connectivityObserver)
    }

    // ── Observer la connectivité et lancer la sync ───────────
    var isOffline by remember { mutableStateOf(!connectivityObserver.isOnline()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        connectivityObserver.observe().collectLatest { online ->
            isOffline = !online
            if (online) {
                // Synchroniser les actions en attente
                scope.launch {
                    try {
                        syncManager.syncPendingActions()
                    } catch (e: Exception) {
                        // Silencieusement ignorer les erreurs de sync
                    }
                }
            }
        }
    }

    // ── Destination de départ ────────────────────────────────
    val startDestination = if (authRepository.isLoggedIn()) AppRoutes.CLUB_LIST else AppRoutes.LOGIN

    // ── Navigation ──────────────────────────────────────────
    NavHost(navController = navController, startDestination = startDestination) {

        // ── Login ────────────────────────────────────────────
        composable(AppRoutes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return LoginViewModel(authRepository) as T
                }
            })

            LaunchedEffect(loginViewModel.loginSuccess) {
                if (loginViewModel.loginSuccess) {
                    Toast.makeText(context, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                    navController.navigate(AppRoutes.CLUB_LIST) {
                        popUpTo(AppRoutes.LOGIN) { inclusive = true }
                    }
                    loginViewModel.resetState()
                }
            }

            LoginScreen(
                isLoading = loginViewModel.isLoading,
                loginError = loginViewModel.loginError,
                onLoginClick = { email, password -> loginViewModel.login(email, password) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Liste des clubs ──────────────────────────────────
        composable(AppRoutes.CLUB_LIST) {
            val clubListViewModel: ClubListViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClubListViewModel(clubRepository) as T
                }
            })

            ClubListScreen(
                clubs = clubListViewModel.clubs,
                isLoading = clubListViewModel.isLoading,
                isOffline = isOffline,
                errorMessage = clubListViewModel.errorMessage,
                onClubClick = { clubId ->
                    navController.navigate(AppRoutes.clubDetail(clubId))
                },
                onRefresh = { clubListViewModel.refreshClubs() },
                onLogoutClick = {
                    authRepository.logout()
                    navController.navigate(AppRoutes.LOGIN) {
                        popUpTo(AppRoutes.CLUB_LIST) { inclusive = true }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // ── Détail d'un club ─────────────────────────────────
        composable(
            route = AppRoutes.CLUB_DETAIL,
            arguments = listOf(navArgument("clubId") { type = NavType.IntType })
        ) { backStackEntry ->
            val clubId = backStackEntry.arguments?.getInt("clubId") ?: 0

            val detailViewModel: ClubDetailViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ClubDetailViewModel(clubId, clubRepository) as T
                }
            })

            ClubDetailScreen(
                club = detailViewModel.club,
                isLoading = detailViewModel.isLoading,
                errorMessage = detailViewModel.errorMessage,
                updateSuccess = detailViewModel.updateSuccess,
                isOffline = isOffline,
                onNavigateBack = { navController.popBackStack() },
                onManageMembersClick = { id ->
                    navController.navigate(AppRoutes.memberManagement(id))
                },
                onUpdateClub = { request -> detailViewModel.updateClub(request) },
                onResetUpdateSuccess = { detailViewModel.resetUpdateSuccess() }
            )
        }

        // ── Gestion des membres ──────────────────────────────
        composable(
            route = AppRoutes.MEMBER_MANAGEMENT,
            arguments = listOf(navArgument("clubId") { type = NavType.IntType })
        ) { backStackEntry ->
            val clubId = backStackEntry.arguments?.getInt("clubId") ?: 0

            val memberViewModel: MemberManagementViewModel = viewModel(factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MemberManagementViewModel(clubId, clubRepository) as T
                }
            })

            MemberManagementScreen(
                approvedMembers = memberViewModel.approvedMembers,
                pendingMembers = memberViewModel.pendingMembers,
                isLoading = memberViewModel.isLoading,
                errorMessage = memberViewModel.errorMessage,
                actionSuccess = memberViewModel.actionSuccess,
                isOffline = isOffline,
                onNavigateBack = { navController.popBackStack() },
                onApproveMember = { userId -> memberViewModel.approveMember(userId) },
                onRejectMember = { userId -> memberViewModel.rejectMember(userId) },
                onRemoveMember = { userId -> memberViewModel.removeMember(userId) },
                onResetActionSuccess = { memberViewModel.resetActionSuccess() },
                onRefresh = { memberViewModel.refreshMembers() }
            )
        }
    }
}