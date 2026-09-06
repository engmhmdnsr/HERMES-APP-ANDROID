package ee.oversight.hermes

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ee.oversight.hermes.ui.MainScreen
import ee.oversight.hermes.ui.theme.HermesTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // The app carries an API key and private conversations. Don't let the
    // recents/app-switcher preview show them (key stays out of screenshots).
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    setContent {
      HermesTheme {
        MainScreen()
      }
    }
  }
}
