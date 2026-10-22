package io.github.clfundev.better.ui.focusrecord

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.clfundev.better.application.BetterApplication
import io.github.clfundev.better.data.repository.FocusRecordRepository

class FocusRecordViewModel(focusRecordRepository: FocusRecordRepository) : ViewModel() {
    val focusRecordsFlow = focusRecordRepository.getAllFocusRecords()

    companion object {
        val factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContainer = (this[APPLICATION_KEY] as BetterApplication).appContainer
                FocusRecordViewModel(
                    focusRecordRepository = appContainer.focusRecordRepository
                )
            }
        }
    }
}