package com.example.digitalspiritlevel.helpers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.VibrationEffect
import android.os.VibratorManager
import androidx.core.content.ContextCompat

sealed interface ActuatorResult {
    data object Success : ActuatorResult
    data object Unavailable : ActuatorResult
    data object PermissionRequired : ActuatorResult
    data class Failure(val message: String) : ActuatorResult
}

class SmartActuator(context: Context) {

    private val appContext = context.applicationContext
    private val vibratorManager =
        appContext.getSystemService(VibratorManager::class.java)
    private val cameraManager =
        appContext.getSystemService(CameraManager::class.java)

    private val flashlightCameraId: String? by lazy {
        if (!appContext.packageManager.hasSystemFeature(
                PackageManager.FEATURE_CAMERA_FLASH
            )
        ) {
            return@lazy null
        }

        try {
            cameraManager.cameraIdList.firstOrNull { cameraId ->
                cameraManager.getCameraCharacteristics(cameraId)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (_: CameraAccessException) {
            null
        }
    }

    val hasVibrator: Boolean
        get() = vibratorManager.defaultVibrator.hasVibrator()

    val hasFlashlight: Boolean
        get() = flashlightCameraId != null

    fun triggerAlarm(): ActuatorResult {
        val vibrator = vibratorManager.defaultVibrator
        if (!vibrator.hasVibrator()) return ActuatorResult.Unavailable

        return try {
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0L, 250L, 120L, 250L),
                -1
            )
            vibrator.vibrate(effect)
            ActuatorResult.Success
        } catch (error: RuntimeException) {
            ActuatorResult.Failure(
                error.message ?: "Не вдалося активувати вібрацію"
            )
        }
    }

    fun stopAlarm() {
        vibratorManager.defaultVibrator.cancel()
    }

    fun toggleFlashlight(enabled: Boolean): ActuatorResult {
        val cameraId = flashlightCameraId ?: return ActuatorResult.Unavailable
        val hasPermission = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return ActuatorResult.PermissionRequired

        return try {
            cameraManager.setTorchMode(cameraId, enabled)
            ActuatorResult.Success
        } catch (error: SecurityException) {
            ActuatorResult.PermissionRequired
        } catch (error: CameraAccessException) {
            ActuatorResult.Failure(
                error.message ?: "Ліхтарик тимчасово недоступний"
            )
        } catch (error: IllegalArgumentException) {
            ActuatorResult.Failure(
                error.message ?: "На пристрої немає доступного ліхтарика"
            )
        }
    }

    fun release() {
        stopAlarm()
        if (hasFlashlight) {
            toggleFlashlight(false)
        }
    }
}
