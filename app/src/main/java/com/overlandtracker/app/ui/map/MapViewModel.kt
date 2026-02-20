package com.overlandtracker.app.ui.map

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.overlandtracker.app.data.thermal.DeviceThermalRepository
import com.overlandtracker.app.data.thermal.DeviceThermalState
import com.overlandtracker.app.data.thermal.ThermalLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapViewModel(application: Application) : AndroidViewModel(application) {
    private val thermalRepository = DeviceThermalRepository(application.applicationContext)

    private val _thermalState = MutableStateFlow(
        DeviceThermalState(
            batteryTemperatureCelsius = null,
            qualitativeStatus = ThermalLevel.UNKNOWN,
            isSupported = false
        )
    )
    val thermalState: StateFlow<DeviceThermalState> = _thermalState.asStateFlow()

    init {
        observeThermalState()
    }

    private fun observeThermalState() {
        viewModelScope.launch {
            while (isActive) {
                _thermalState.value = withContext(Dispatchers.IO) {
                    thermalRepository.readThermalState()
                }
                delay(30_000)
            }
        }
    }
}
