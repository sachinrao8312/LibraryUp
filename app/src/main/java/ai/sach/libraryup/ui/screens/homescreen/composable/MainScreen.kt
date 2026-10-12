package ai.sach.rentup.ui.screens.homescreen.composable

import ai.sach.rentup.data.viewmodel.TenantViewModel
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import android.util.Log
import ai.sach.rentup.BuildConfig

fun shareOnboardingLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Please fill out your onboarding form: $url")
    }
    context.startActivity(Intent.createChooser(intent, "Send onboarding link via"))
}

private fun logDebug(tag: String, message: String) {
    if (BuildConfig.DEBUG) android.util.Log.d(tag, message)
}

@Composable
fun MainScreen(viewModel: TenantViewModel) {
    val onboardingLinkResult by viewModel.onboardingLinkResult.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showShareDialog by remember { mutableStateOf(false) }
    var linkToShare by remember { mutableStateOf("") }

    // Show onboarding link result and offer to share
    LaunchedEffect(onboardingLinkResult) {
        onboardingLinkResult?.url?.let { url ->
            linkToShare = url
            showShareDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(Modifier.padding(padding)) {

        }
        // Share dialog for onboarding link
        if (showShareDialog && linkToShare.isNotBlank()) {
            AlertDialog(
                onDismissRequest = { showShareDialog = false },
                title = { Text("Onboarding Link Ready") },
                text = { Text(linkToShare) },
                confirmButton = {
                    Button(onClick = {
                        shareOnboardingLink(context, linkToShare)
                        showShareDialog = false
                    }) { Text("Share Link") }
                },
                dismissButton = {
                    TextButton(onClick = { showShareDialog = false }) { Text("Close") }
                }
            )
        }
    }
} 