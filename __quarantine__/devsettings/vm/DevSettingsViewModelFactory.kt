package com.zak.pressmark.feature.devsettings.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.zak.pressmark.data.repository.v1.DevSettingsRepository

class DevSettingsViewModelFactory(
    private val repository: DevSettingsRepository,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DevSettingsViewModel(repository) as T
    }
}
