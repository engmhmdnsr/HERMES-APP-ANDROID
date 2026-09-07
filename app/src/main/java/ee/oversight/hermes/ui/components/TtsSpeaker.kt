package ee.oversight.hermes.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ee.oversight.hermes.ui.theme.NeonCyan
import ee.oversight.hermes.ui.theme.TextSecondary
import java.util.Locale

/**
 * Small speaker button next to an assistant reply: reads the text aloud
 * using the device's built-in TextToSpeech engine (offline, no API key).
 */
@Composable
fun TtsSpeaker(
    text: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var speaking by remember { mutableStateOf(false) }
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ready by remember { mutableStateOf(false) }

    fun speak(engine: TextToSpeech) {
        engine.language = Locale.getDefault()
        val utteranceId = "hermes_tts_${System.currentTimeMillis()}"
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                speaking = false
            }
            override fun onError(utteranceId: String?) {
                speaking = false
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?, errorCode: Int) {
                speaking = false
            }
        })
        speaking = true
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    // Create engine once per composition, release on dispose.
    DisposableEffect(Unit) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
            }
        }
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
        }
    }

    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (speaking) NeonCyan.copy(alpha = 0.2f) else Color(0xFF141A26))
            .border(1.dp, if (speaking) NeonCyan else Color(0xFF2A3448), CircleShape)
            .clickable(enabled = text.isNotBlank()) {
                val engine = tts
                if (engine != null) {
                    if (speaking) {
                        engine.stop()
                        speaking = false
                    } else {
                        speak(engine)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Speak reply",
            tint = if (speaking) NeonCyan else TextSecondary,
            modifier = Modifier.size(14.dp)
        )
    }
}
