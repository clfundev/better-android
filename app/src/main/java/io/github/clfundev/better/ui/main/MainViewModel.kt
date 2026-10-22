package io.github.clfundev.better.ui.main

import androidx.lifecycle.ViewModel
import io.github.clfundev.better.service.FocusService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private val _focusServiceFlow = MutableStateFlow<FocusService?>(null)

    val focusServiceFlow = _focusServiceFlow.asStateFlow()

    // 只由MainActivity调用
    fun setFocusService(focusService: FocusService?) {
        _focusServiceFlow.value = focusService
    }
}
