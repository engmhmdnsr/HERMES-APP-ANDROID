package ee.oversight.hermes

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import ee.oversight.hermes.ui.MainScreen
import ee.oversight.hermes.ui.theme.HermesTheme

class MainActivity : FragmentActivity() {

  private val requestNotifPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { /* granted or not — notifications are best-effort */ }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // The app carries an API key and private conversations. Don't let the
    // recents/app-switcher preview show them (key stays out of screenshots).
    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

    // Android 13+ needs explicit notification permission for reply-done and
    // approval notifications. Ask once on first launch.
    if (Build.VERSION.SDK_INT >= 33) {
      val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED
      if (!granted) {
        requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }

    setContent {
      HermesTheme {
        MainScreen()
      }
    }
  }
}
