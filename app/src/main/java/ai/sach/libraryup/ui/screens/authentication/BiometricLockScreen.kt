package ai.sach.libraryup.ui.screens.authentication

import ai.sach.libraryup.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

/**
 * App lock: asks for the fingerprint as soon as it shows. Only shown when biometrics are
 * available and turned on; if that changes underneath us the app simply unlocks.
 */
@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    biometricAuthManager: BiometricAuthManager? = null,
    onAuthenticationSuccess: () -> Unit,
    onSkipAuthentication: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = biometricAuthManager ?: remember { BiometricAuthManager(context) }
    val available by auth.isBiometricAvailable.collectAsState()
    val enabled by auth.isBiometricEnabled.collectAsState()
    val title = stringResource(R.string.lock_prompt_title)
    val subtitle = stringResource(R.string.lock_prompt_subtitle)
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun prompt() {
        if (busy) return
        if (!enabled || !available) return onSkipAuthentication()
        busy = true
        error = null
        auth.showBiometricPrompt(
            activity = activity, title = title, subtitle = subtitle,
            onSuccess = { busy = false; onAuthenticationSuccess() },
            onError = { busy = false; error = it },
            onCancel = { busy = false }
        )
    }
    LaunchedEffect(enabled) { prompt() }

    LockContent(error, onUnlock = ::prompt)
}

/** Stateless lock layout (state hoisted for screenshot tests). */
@Composable
fun LockContent(error: String?, onUnlock: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AppIcon(Modifier.size(72.dp))
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(56.dp))
            FilledTonalIconButton(onClick = onUnlock, modifier = Modifier.size(96.dp)) {
                Icon(Icons.Default.Fingerprint, contentDescription = stringResource(R.string.lock_fingerprint), modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(if (error == null) R.string.lock_touch_sensor else R.string.lock_failed),
                style = MaterialTheme.typography.bodyLarge,
                color = if (error == null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
