package ee.oversight.hermes.ui.components

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ee.oversight.hermes.ui.theme.CyberBg
import ee.oversight.hermes.ui.theme.MonospaceStyle
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.TextSecondary

/**
 * Full-screen biometric gate shown when app-lock is enabled. Blocks the UI
 * until the user authenticates (fingerprint / face).
 */
@Composable
fun BiometricLockGate(onUnlocked: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity

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
            Text(
                "Unlock with your fingerprint or face",
                style = MonospaceStyle.copy(fontSize = 12.sp, color = TextSecondary)
            )
        }
    }

    LaunchedEffect(Unit) {
        if (activity == null) {
            // No FragmentActivity context (unlikely) — just unlock.
            onUnlocked()
            return@LaunchedEffect
        }
        val biometricManager = BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometrics enrolled — fall back to allowing entry (device likely
            // has a PIN; DEVICE_CREDENTIAL covers that) or just unlock.
            onUnlocked()
            return@LaunchedEffect
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // User cancelled — keep the gate up (they can retry by
                    // tapping, or we re-show the prompt on next resume).
                }
            }
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Hermes Control")
                .setSubtitle("Verify your identity")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build()
        )
    }
}
