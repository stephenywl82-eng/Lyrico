package com.lonx.lyrico.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lonx.lyrico.data.repository.SettingsRepository
import com.lonx.lyrico.utils.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

enum class ProVersionTapResult { NONE, ACTIVATED, CONFIRM_DEACTIVATION }

class AboutViewModel(
    private val settingsRepository: SettingsRepository,
    private val updateManager: UpdateManager,
) : ViewModel() {
    val proActivated = settingsRepository.proActivated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    private val versionTapMutex = Mutex()
    private var versionTapCount = 0
    private var lastVersionTap = 0L

    /** Only successive taps count; persistence must succeed before reporting activation. */
    suspend fun onVersionTap(): ProVersionTapResult = versionTapMutex.withLock {
        val now = SystemClock.elapsedRealtime()
        versionTapCount = if (now - lastVersionTap <= 1500L) versionTapCount + 1 else 1
        lastVersionTap = now
        if (versionTapCount < 7) return@withLock ProVersionTapResult.NONE
        versionTapCount = 0
        if (settingsRepository.proActivated.first()) return@withLock ProVersionTapResult.CONFIRM_DEACTIVATION
        settingsRepository.activatePro()
        ProVersionTapResult.ACTIVATED
    }

    val updateEffect = updateManager.effect
    fun openBrowser(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun checkUpdate() {
        viewModelScope.launch {
            updateManager.checkForUpdate()
        }
    }
}
