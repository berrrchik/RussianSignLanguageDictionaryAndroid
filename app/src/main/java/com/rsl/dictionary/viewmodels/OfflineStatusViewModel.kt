package com.rsl.dictionary.viewmodels

import androidx.lifecycle.ViewModel
import com.rsl.dictionary.services.network.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class OfflineStatusViewModel @Inject constructor(
    networkMonitor: NetworkMonitor
) : ViewModel() {
    val isNetworkConnected: StateFlow<Boolean> = networkMonitor.isConnectedFlow
}
