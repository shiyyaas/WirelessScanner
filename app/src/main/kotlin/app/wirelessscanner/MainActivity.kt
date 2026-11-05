package app.wirelessscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.wirelessscanner.ui.navigation.AppNavHost
import app.wirelessscanner.ui.theme.WirelessScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WirelessScannerTheme {
                AppNavHost()
            }
        }
    }
}
