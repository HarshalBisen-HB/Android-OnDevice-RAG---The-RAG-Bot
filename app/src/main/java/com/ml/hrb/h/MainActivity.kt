// Author: Harshal R. Bisen
// Main entry point and compose activity of the Android application.

package com.ml.hrb.h

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ml.hrb.h.ui.screens.chat.ChatNavEvent
import com.ml.hrb.h.ui.screens.chat.ChatScreen
import com.ml.hrb.h.ui.screens.chat.ChatViewModel
import com.ml.hrb.h.ui.screens.docs.DocsScreen
import com.ml.hrb.h.ui.screens.docs.DocsViewModel
import com.ml.hrb.h.ui.screens.edit_credentials.EditCredentialsScreen
import com.ml.hrb.h.ui.screens.local_models.LocalModelsScreen
import com.ml.hrb.h.ui.screens.local_models.LocalModelsViewModel
import kotlinx.serialization.Serializable
import org.koin.androidx.compose.koinViewModel

@Serializable
object ChatRoute

@Serializable
object EditAPIKeyRoute

@Serializable
object DocsRoute

@Serializable
object LocalModelsRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = ChatRoute,
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable<DocsRoute> { backStackEntry ->
                    val viewModel: DocsViewModel =
                        koinViewModel(viewModelStoreOwner = backStackEntry)
                    val docScreenUIState by viewModel.docsScreenUIState.collectAsState()
                    DocsScreen(
                        docScreenUIState,
                        onBackClick = { navHostController.navigateUp() },
                        onEvent = viewModel::onEvent,
                    )
                }
                composable<EditAPIKeyRoute> { EditCredentialsScreen(onBackClick = { navHostController.navigateUp() }) }
                composable<LocalModelsRoute> { backStackEntry ->
                    val viewModel: LocalModelsViewModel =
                        koinViewModel(viewModelStoreOwner = backStackEntry)
                    val uiState by viewModel.uiState.collectAsState()
                    LocalModelsScreen(
                        uiState = uiState,
                        onEvent = viewModel::onEvent,
                        onBackClick = { navHostController.navigateUp() },
                    )
                }
                composable<ChatRoute> { backStackEntry ->
                    val viewModel: ChatViewModel =
                        koinViewModel(viewModelStoreOwner = backStackEntry)
                    val chatScreenUIState by viewModel.chatScreenUIState.collectAsState()
                    val navEvent by viewModel.navEventChannel.collectAsState(ChatNavEvent.None)
                    LaunchedEffect(navEvent) {
                        when (navEvent) {
                            is ChatNavEvent.ToDocsScreen -> {
                                navHostController.navigate(DocsRoute)
                            }

                            is ChatNavEvent.ToEditAPIKeyScreen -> {
                                navHostController.navigate(EditAPIKeyRoute)
                            }

                            is ChatNavEvent.ToLocalModelsScreen -> {
                                navHostController.navigate(LocalModelsRoute)
                            }

                            is ChatNavEvent.None -> {}
                        }
                    }
                    ChatScreen(
                        screenUiState = chatScreenUIState,
                        onScreenEvent = { viewModel.onChatScreenEvent(it) },
                    )
                }
            }
        }
    }
}
