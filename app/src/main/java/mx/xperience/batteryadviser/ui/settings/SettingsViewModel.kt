package mx.xperience.batteryadviser.ui.settings

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mx.xperience.batteryadviser.ui.theme.ThemeMode

class SettingsViewModel : ViewModel() {

    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
    }
}
