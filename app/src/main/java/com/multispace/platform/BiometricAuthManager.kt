package com.multispace.platform

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.multispace.diagnostics.AppLogger

/**
 * Platform helper for hardware biometric verification (Fingerprint, Face Unlock, Device Credential).
 * Handles capability probing, graceful fallback, and BiometricPrompt orchestration.
 */
object BiometricAuthManager {

  enum class BiometricStatus {
    AVAILABLE,
    NOT_ENROLLED,
    NO_HARDWARE,
    HW_UNAVAILABLE,
    SECURITY_UPDATE_REQUIRED,
    UNSUPPORTED
  }

  /**
   * Checks whether hardware biometric authentication (Strong/Weak) is supported and configured on device.
   */
  fun checkBiometricStatus(context: Context): BiometricStatus {
    return try {
      val biometricManager = BiometricManager.from(context)
      val authenticators = BIOMETRIC_STRONG or BIOMETRIC_WEAK
      when (biometricManager.canAuthenticate(authenticators)) {
        BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.HW_UNAVAILABLE
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricStatus.SECURITY_UPDATE_REQUIRED
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> BiometricStatus.UNSUPPORTED
        else -> BiometricStatus.UNSUPPORTED
      }
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.AUTH, "Failed checking biometric status: ${e.message}")
      BiometricStatus.UNSUPPORTED
    }
  }

  fun isBiometricAvailable(context: Context): Boolean {
    return checkBiometricStatus(context) == BiometricStatus.AVAILABLE
  }

  fun getStatusDescription(status: BiometricStatus): String {
    return when (status) {
      BiometricStatus.AVAILABLE -> "Biometric hardware ready & enrolled"
      BiometricStatus.NOT_ENROLLED -> "Biometrics supported but no fingerprints/face registered"
      BiometricStatus.NO_HARDWARE -> "No biometric hardware detected on this device"
      BiometricStatus.HW_UNAVAILABLE -> "Biometric hardware is currently unavailable"
      BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Security update required for biometric sensors"
      BiometricStatus.UNSUPPORTED -> "Biometric authentication is not supported"
    }
  }

  /**
   * Displays the system biometric prompt with the specified parameters.
   */
  fun authenticate(
    activity: FragmentActivity,
    title: String = "Multi-Space Authentication",
    subtitle: String = "Verify your identity to proceed",
    description: String = "",
    negativeButtonText: String = "Use PIN / Pattern",
    onSuccess: (BiometricPrompt.AuthenticationResult) -> Unit,
    onError: (errorCode: Int, errString: CharSequence) -> Unit = { _, _ -> },
    onFailed: () -> Unit = {}
  ) {
    try {
      val executor = ContextCompat.getMainExecutor(activity)
      val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
          super.onAuthenticationSucceeded(result)
          AppLogger.i(AppLogger.Category.AUTH, "Biometric authentication succeeded")
          onSuccess(result)
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
          super.onAuthenticationError(errorCode, errString)
          AppLogger.w(AppLogger.Category.AUTH, "Biometric authentication error ($errorCode): $errString")
          onError(errorCode, errString)
        }

        override fun onAuthenticationFailed() {
          super.onAuthenticationFailed()
          AppLogger.w(AppLogger.Category.AUTH, "Biometric authentication failed (fingerprint not recognized)")
          onFailed()
        }
      }

      val biometricPrompt = BiometricPrompt(activity, executor, promptCallback)

      val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)

      if (description.isNotEmpty()) {
        promptInfoBuilder.setDescription(description)
      }

      promptInfoBuilder.setNegativeButtonText(negativeButtonText)
      promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK)

      val promptInfo = promptInfoBuilder.build()
      biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.AUTH, "Failed launching BiometricPrompt: ${e.message}", e)
      onError(-1, e.message ?: "Biometric prompt error")
    }
  }

  /**
   * Traverses Context hierarchy to locate a hosting FragmentActivity.
   */
  fun findFragmentActivity(context: Context): FragmentActivity? {
    var ctx: Context? = context
    while (ctx != null) {
      if (ctx is FragmentActivity) {
        return ctx
      }
      if (ctx is ContextWrapper) {
        ctx = ctx.baseContext
      } else {
        break
      }
    }
    return null
  }
}
