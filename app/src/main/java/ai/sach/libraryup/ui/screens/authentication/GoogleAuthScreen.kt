package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.R
import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.EventSeat
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn

/** First screen for a new owner: what LibraryUp does, and "Continue with Google" (or skip as guest). */
@Composable
fun GoogleAuthScreen(authViewModel: AuthViewModel, onNavigateToMain: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val authState by authViewModel.authState.collectAsState()
    val ui by authViewModel.authUiState.collectAsState()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) runCatching {
            authViewModel.processGoogleSignIn(GoogleSignIn.getSignedInAccountFromIntent(result.data), context)
        }.onFailure { Log.e("GoogleAuthScreen", "Google sign-in result", it) }
    }
    if (authState is AuthState.Authenticated || authState is AuthState.Guest) {
        onNavigateToMain()
        return
    }

    SignInContent(
        loading = ui.isLoading,
        error = ui.error,
        onSkip = { authViewModel.signInAsGuest(context) },
        onGoogle = { authViewModel.getGoogleSignInIntent()?.let(launcher::launch) },
        modifier = modifier
    )
}

/** Stateless sign-in layout (state hoisted for previews and screenshot tests). */
@Composable
fun SignInContent(loading: Boolean, error: String?, onSkip: () -> Unit, onGoogle: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 24.dp)) {
        TextButton(onClick = onSkip, enabled = !loading, modifier = Modifier.align(Alignment.End)) {
            Text(stringResource(R.string.action_skip))
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(24.dp))
            AppIcon(Modifier.size(96.dp))
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.welcome_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(40.dp))
            Point(Icons.Outlined.EventSeat, stringResource(R.string.auth_point_seats))
            Point(Icons.Outlined.CurrencyRupee, stringResource(R.string.auth_point_fees))
            Point(Icons.Outlined.Lock, stringResource(R.string.auth_point_private))
        }

        error?.let {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                Text(it, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
            }
            Spacer(Modifier.height(12.dp))
        }
        OutlinedButton(
            onClick = onGoogle,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Image(painterResource(R.drawable.ic_google), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.sign_in_with_google), style = MaterialTheme.typography.titleSmall)
        }
        Text(
            stringResource(R.string.auth_footer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        )
    }
}

/** The launcher icon (background + foreground layers) as a rounded tile. */
@Composable
fun AppIcon(modifier: Modifier = Modifier) {
    Box(modifier.clip(MaterialTheme.shapes.extraLarge)) {
        Image(painterResource(R.drawable.ic_launcher_background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = stringResource(R.string.app_logo_content_description), modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Point(icon: ImageVector, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
