package mx.xperience.batteryadviser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import mx.xperience.batteryadviser.ui.BatteryViewModel
import mx.xperience.batteryadviser.ui.screens.MainScreen
import mx.xperience.batteryadviser.ui.theme.BatteryTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // Usamos el ViewModel y la Screen
            val vm: mx.xperience.batteryadviser.ui.BatteryViewModel = viewModel()
            MainScreen(viewModel = vm)
        }
    }
}