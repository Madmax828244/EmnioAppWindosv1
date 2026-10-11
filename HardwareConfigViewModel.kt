package com.emnio.windowspc.viewmodel

import androidx.lifecycle.ViewModel
import com.emnio.windowspc.model.HardwareConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class HardwareConfigViewModel : ViewModel() {
    private val _config = MutableStateFlow(HardwareConfig())
    val config: StateFlow<HardwareConfig> = _config.asStateFlow()

    fun update(transform: (HardwareConfig) -> HardwareConfig) = _config.update(transform)
}
