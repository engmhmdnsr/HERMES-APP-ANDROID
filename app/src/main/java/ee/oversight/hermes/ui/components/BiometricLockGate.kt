package ee.oversight.hermes.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.NeonRed
import ee.oversight.hermes.ui.theme.TextSecondary

/**
 * Full-screen biometric gate shown when app-lock is enabled. Blocks the UI
 * until the user authenticates (fingerprint / face / device credential).
 *
 * If the device has NO biometrics enrolled AND no device credential (PIN /
 * pattern / password), the gate does NOT silently unlock: it shows a message
 * telling the user to set up a screen lock, with a button to open the system
 * security settings. A "Try again" button re-launches the prompt after a
 * failed / cancelled attempt.
 */
@Composable
fun BiometricLockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    var needsSetup by remember { mutableStateOf(false) }
    var showRetry by remember { mutableStateOf(false) }
    var setupChecked by remember { mutableStateOf(false) }
    // Increment to re-run the LaunchedEffect below (retry).
    var attempt by remember { mutableStateOf(0) }

    // Re-check setup state whenever an auth attempt completes or the gate
    // recomposes after resume.
    LaunchedEffect(attempt, setupChecked) {
        if (setupChecked) return@LaunchedEffect
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        needsSetup = canAuth != BiometricManager.BIOMETRIC_SUCCESS
        setupChecked = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔒", style = MonospaceStyle.copy(fontSize = 44.sp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Hermes Control",
                style = MonospaceStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
            )
            Spacer(Modifier.height(8.dp))

            if (needsSetup) {
                // No usable credential on the device: stay locked, tell the
                // user how to fix it. Never silently unlock.
                Text(
                    "No biometrics or screen lock are set up on this device.\n\n" +
                        "App lock cannot work without a secure lock screen. " +
                        "Set up a fingerprint, face, PIN or pattern, then come back.",
                    style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Open security settings", style = MonospaceStyle.copy(fontSize = 13.sp))
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    // Re-check in case the user just set up a lock and returned.
                    setupChecked = false
                    attempt += 1
                }) {
                    Text("I set it up — check again", style = MonospaceStyle.copy(fontSize = 12.sp, color = NeonCyan))
                }
            } else {
                // Prompt text area; a retry affordance shows after a failed
                // or cancelled attempt.
                Text(
                    if (showRetry)
                        "Authentication failed or cancelled. Try again."
                    else
                        "Unlock with your fingerprint, face, PIN or pattern",
                    style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                if (showRetry) {
                    Button(
                        onClick = { attempt += 1 },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Try again", style = MonospaceStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "or unlock via app settings",
                        style = MonospaceStyle.copy(fontSize = 11.sp, color = TextSecondary)
                    )
                }
            }
        }
    }

    LaunchedEffect(activity, attempt) {
        if (activity == null) {
            // No FragmentActivity context (unlikely) — keep the gate up with
            // a message instead of unlocking: a lock that silently opens is
            // worse than none. Fall back to letting the retry path handle it.
            needsSetup = true
            return@LaunchedEffect
        }
        val bm = BiometricManager.from(context)
        val canAuth = bm.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            needsSetup = true
            return@LaunchedEffect
        }
        needsSetup = false
        showRetry = false

        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled or failed — show a retry button.
                    showRetry = true
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hermes Control")
                .setSubtitle("Verify your identity")
                .setAllowedAuthenticators(
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
                )
                .build()
        )
    }
}
