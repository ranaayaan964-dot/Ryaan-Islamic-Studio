package com.example.util

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State representing the GitHub In-App Update status.
 */
data class UpdateUiState(
    val showUpdateDialog: Boolean = false,
    val newVersionName: String = "",
    val downloadUrl: String = "",
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val isChecking: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Production-ready In-App Update ViewModel.
 *
 * Real-time GitHub Release checker that queries:
 * https://api.github.com/repos/ranaayaan964-dot/Ryaan-Islamic-Studio/releases/latest
 *
 * Pure live logic: zero mock data, zero simulated timers, and zero fake versions.
 */
class UpdateViewModel : ViewModel() {

    companion object {
        private const val TAG = "UpdateViewModel"
    }

    private val _uiState = MutableStateFlow(UpdateUiState())
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private val _newVersionName = MutableStateFlow("")
    val newVersionName: StateFlow<String> = _newVersionName.asStateFlow()

    private val _downloadUrl = MutableStateFlow("")
    val downloadUrl: StateFlow<String> = _downloadUrl.asStateFlow()

    init {
        checkForUpdates()
    }

    /**
     * Queries the live GitHub Releases API in a background coroutine on Dispatchers.IO.
     * Evaluates whether an update is available using semantic version comparison.
     */
    fun checkForUpdates() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            Log.d(TAG, "Initiating live GitHub update check. Installed version: ${BuildConfig.VERSION_NAME}")

            when (val result = AppUpdateManager.checkForUpdates()) {
                is UpdateCheckResult.UpdateAvailable -> {
                    Log.i(TAG, "Update available from GitHub: ${result.info.latestVersionName}")
                    _showUpdateDialog.value = true
                    _newVersionName.value = result.info.latestVersionName
                    _downloadUrl.value = result.info.downloadUrl

                    _uiState.update {
                        it.copy(
                            showUpdateDialog = true,
                            newVersionName = result.info.latestVersionName,
                            downloadUrl = result.info.downloadUrl,
                            isChecking = false,
                            errorMessage = null
                        )
                    }
                }
                is UpdateCheckResult.UpToDate -> {
                    Log.d(TAG, "Application is up to date.")
                    _showUpdateDialog.value = false
                    _uiState.update {
                        it.copy(
                            showUpdateDialog = false,
                            isChecking = false,
                            errorMessage = null
                        )
                    }
                }
                is UpdateCheckResult.Error -> {
                    Log.w(TAG, "Update check notice: ${result.message}")
                    _showUpdateDialog.value = false
                    _uiState.update {
                        it.copy(
                            showUpdateDialog = false,
                            isChecking = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun dismissDialog() {
        _showUpdateDialog.value = false
        _uiState.update { it.copy(showUpdateDialog = false) }
    }
}
