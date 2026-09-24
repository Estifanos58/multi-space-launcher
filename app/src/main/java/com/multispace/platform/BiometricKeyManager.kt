package com.multispace.platform

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import com.multispace.diagnostics.AppLogger
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Manages Android Keystore keys for biometric-protected Spaces and wraps operations
 * in [BiometricPrompt.CryptoObject] to ensure cryptographic binding to hardware authenticators.
 */
object BiometricKeyManager {

  private const val ANDROID_KEYSTORE = "AndroidKeyStore"
  private const val KEY_ALIAS_PREFIX = "multispace_space_auth_"
  private const val CIPHER_TRANSFORMATION = "AES/CBC/PKCS7Padding"

  sealed interface CryptoInitResult {
    data class Success(val cryptoObject: BiometricPrompt.CryptoObject) : CryptoInitResult
    data object KeyPermanentlyInvalidated : CryptoInitResult
    data class Error(val message: String, val cause: Throwable? = null) : CryptoInitResult
  }

  fun getKeyAlias(spaceId: String): String {
    return "$KEY_ALIAS_PREFIX$spaceId"
  }

  /**
   * Generates or retrieves an AES-256 secret key in AndroidKeyStore requiring biometric authentication.
   */
  fun getOrCreateSecretKey(spaceId: String): Result<SecretKey> {
    val alias = getKeyAlias(spaceId)
    return try {
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)

      if (keyStore.containsAlias(alias)) {
        val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
        if (entry != null) {
          return Result.success(entry.secretKey)
        }
      }

      val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
      val builder = KeyGenParameterSpec.Builder(
        alias,
        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
      )
        .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
        .setKeySize(256)
        .setUserAuthenticationRequired(true)

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
      }

      keyGenerator.init(builder.build())
      val key = keyGenerator.generateKey()
      AppLogger.i(AppLogger.Category.AUTH, "Generated hardware Keystore key for Space ($spaceId)")
      Result.success(key)
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.AUTH, "Failed to create/get Keystore key for Space ($spaceId): ${e.message}")
      Result.failure(e)
    }
  }

  /**
   * Initializes a cipher and returns a [BiometricPrompt.CryptoObject].
   * Handles [KeyPermanentlyInvalidatedException] if biometric enrollment changed on device.
   */
  fun createCryptoObject(spaceId: String): CryptoInitResult {
    val keyResult = getOrCreateSecretKey(spaceId)
    val secretKey = keyResult.getOrElse {
      return CryptoInitResult.Error("Unable to access Keystore key for Space $spaceId", it)
    }

    return try {
      val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
      cipher.init(Cipher.ENCRYPT_MODE, secretKey)
      CryptoInitResult.Success(BiometricPrompt.CryptoObject(cipher))
    } catch (e: KeyPermanentlyInvalidatedException) {
      AppLogger.w(AppLogger.Category.AUTH, "Biometric key permanently invalidated for Space ($spaceId)")
      CryptoInitResult.KeyPermanentlyInvalidated
    } catch (e: Exception) {
      AppLogger.e(AppLogger.Category.AUTH, "Failed initializing cipher for Space ($spaceId): ${e.message}", e)
      CryptoInitResult.Error("Failed initializing biometric cipher: ${e.message}", e)
    }
  }

  /**
   * Deletes the Keystore key for the given space (e.g. when space is deleted or security disabled).
   */
  fun deleteSecretKey(spaceId: String) {
    try {
      val alias = getKeyAlias(spaceId)
      val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
      keyStore.load(null)
      if (keyStore.containsAlias(alias)) {
        keyStore.deleteEntry(alias)
        AppLogger.i(AppLogger.Category.AUTH, "Deleted Keystore key for Space ($spaceId)")
      }
    } catch (e: Exception) {
      AppLogger.w(AppLogger.Category.AUTH, "Failed to delete Keystore key for Space ($spaceId): ${e.message}")
    }
  }
}
