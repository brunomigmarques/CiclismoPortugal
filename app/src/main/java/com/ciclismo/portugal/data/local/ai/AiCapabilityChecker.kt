package com.ciclismo.portugal.data.local.ai

import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Verifica se o dispositivo suporta AI on-device (Gemini Nano).
 * Requisitos: Android 14+ com Google AI Core instalado.
 */
@Singleton
class AiCapabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AiCapabilityChecker"
        private const val MIN_SDK_FOR_ON_DEVICE = Build.VERSION_CODES.UPSIDE_DOWN_CAKE // API 34 (Android 14)
        private const val AI_CORE_PACKAGE = "com.google.android.aicore"
    }

    /**
     * Verifica se o dispositivo suporta Gemini Nano on-device.
     */
    fun isOnDeviceAiSupported(): Boolean {
        // Check Android version (requires Android 14+)
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_ON_DEVICE) {
            Log.d(TAG, "Device SDK ${Build.VERSION.SDK_INT} < $MIN_SDK_FOR_ON_DEVICE, on-device AI not supported")
            return false
        }

        // Check if Google AI Core is installed
        return try {
            context.packageManager.getPackageInfo(AI_CORE_PACKAGE, 0)
            Log.d(TAG, "Google AI Core found, on-device AI supported")
            true
        } catch (e: Exception) {
            Log.d(TAG, "Google AI Core not found: ${e.message}")
            false
        }
    }

    /**
     * Obtem informacoes sobre as capacidades de AI do dispositivo.
     */
    fun getAiCapabilities(): AiCapabilities {
        val sdkVersion = Build.VERSION.SDK_INT
        val isOnDeviceSupported = isOnDeviceAiSupported()

        return AiCapabilities(
            androidVersion = sdkVersion,
            isOnDeviceAiSupported = isOnDeviceSupported,
            recommendedMode = if (isOnDeviceSupported) AiMode.ON_DEVICE else AiMode.CLOUD,
            aiCoreInstalled = isAiCoreInstalled()
        )
    }

    /**
     * Verifica se o Google AI Core esta instalado.
     */
    private fun isAiCoreInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(AI_CORE_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Obtem a versao do Android formatada.
     */
    fun getAndroidVersionName(): String {
        return when (Build.VERSION.SDK_INT) {
            Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> "Android 14"
            Build.VERSION_CODES.TIRAMISU -> "Android 13"
            Build.VERSION_CODES.S_V2 -> "Android 12L"
            Build.VERSION_CODES.S -> "Android 12"
            Build.VERSION_CODES.R -> "Android 11"
            Build.VERSION_CODES.Q -> "Android 10"
            else -> "Android ${Build.VERSION.SDK_INT}"
        }
    }
}

/**
 * Capacidades de AI do dispositivo.
 */
data class AiCapabilities(
    val androidVersion: Int,
    val isOnDeviceAiSupported: Boolean,
    val recommendedMode: AiMode,
    val aiCoreInstalled: Boolean
)

/**
 * Modo de AI disponivel.
 */
enum class AiMode {
    ON_DEVICE, // Gemini Nano on-device
    CLOUD,     // Gemini API cloud fallback
    DISABLED   // AI desativado pelo utilizador
}
