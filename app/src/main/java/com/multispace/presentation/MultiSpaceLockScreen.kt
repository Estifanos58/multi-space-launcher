package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.multispace.R
import com.multispace.domain.model.Space
import com.multispace.domain.model.WallpaperCatalog
import com.multispace.platform.BiometricAuthManager
import com.multispace.platform.BiometricKeyManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modern Full-Screen Lock Screen for MultiSpace Launcher.
 * Features:
 * - Explicit target space selection (No cross-space global match leakage)
 * - True cryptographic Biometric unlock via Android Keystore CryptoObject
 * - Fallback Recovery PIN path with automatic Keystore re-enrollment
 * - Persistent rate-limiting & backoff lockout cooldown indicator
 * - Support for PIN, Gesture Pattern, Biometrics, or Guest bypass
 */
@Composable
fun MultiSpaceLockScreen(
  spaceViewModel: SpaceViewModel,
  onUnlockSuccess: (Space) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val allSpaces by spaceViewModel.allSpaces.collectAsState()
  val activeSpace by spaceViewModel.activeSpace.collectAsState()

  var inputMode by remember { mutableStateOf("PIN") } // "PIN", "PATTERN", "RECOVERY_PIN"
  var enteredPin by remember { mutableStateOf("") }
  var isVerifying by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successSpaceName by remember { mutableStateOf<String?>(null) }
  var patternClearTrigger by remember { mutableIntStateOf(0) }

  val coroutineScope = rememberCoroutineScope()
  val fragmentActivity = remember(context) { BiometricAuthManager.findFragmentActivity(context) }
  val isBiometricAvailable = remember(context) { BiometricAuthManager.isStrongBiometricAvailable(context) }

  var selectedTargetSpaceId by remember { mutableStateOf<String?>(null) }
  var cooldownSeconds by remember { mutableStateOf<Long?>(null) }
  var isKeyInvalidated by remember { mutableStateOf(false) }

  // Target space: explicitly selected space, or currently active space, or first space
  val targetSpace = remember(allSpaces, activeSpace, selectedTargetSpaceId) {
    if (selectedTargetSpaceId != null) {
      allSpaces.firstOrNull { it.id == selectedTargetSpaceId }
    } else {
      activeSpace ?: allSpaces.firstOrNull { it.isProtected } ?: allSpaces.firstOrNull()
    }
  }

  // Synchronize input mode with target space security policy
  LaunchedEffect(targetSpace?.id) {
    val space = targetSpace ?: return@LaunchedEffect
    enteredPin = ""
    errorMessage = null
    isKeyInvalidated = false
    when {
      space.isPatternProtected || space.authPolicy == Space.AUTH_PATTERN -> inputMode = "PATTERN"
      space.isBiometricProtected || space.authPolicy == Space.AUTH_BIOMETRIC -> inputMode = "PIN"
      else -> inputMode = "PIN"
    }
  }

  // Periodic lockout cooldown monitor
  LaunchedEffect(targetSpace?.id, isVerifying) {
    while (true) {
      val spaceId = targetSpace?.id
      if (spaceId != null) {
        val remaining = spaceViewModel.getRemainingCooldownSeconds(spaceId)
        cooldownSeconds = remaining
        if (remaining != null && remaining > 0) {
          errorMessage = "Too many attempts. Cooldown: ${remaining}s"
        } else if (errorMessage?.startsWith("Too many attempts") == true) {
          errorMessage = null
        }
      }
      delay(1000)
    }
  }

  val isLockedOut = (cooldownSeconds != null && cooldownSeconds!! > 0)

  fun triggerBiometricPrompt() {
    if (fragmentActivity == null || !isBiometricAvailable) return
    val space = targetSpace ?: return
    if (isLockedOut) return

    val cryptoResult = BiometricKeyManager.createCryptoObject(space.id)
    val cryptoObject = when (cryptoResult) {
      is BiometricKeyManager.CryptoInitResult.Success -> cryptoResult.cryptoObject
      is BiometricKeyManager.CryptoInitResult.KeyPermanentlyInvalidated -> {
        isKeyInvalidated = true
        inputMode = "RECOVERY_PIN"
        errorMessage = "Biometrics changed on device. Enter Recovery PIN to re-enroll."
        return
      }
      is BiometricKeyManager.CryptoInitResult.Error -> {
        inputMode = "RECOVERY_PIN"
        errorMessage = "Biometric security error. Enter Recovery PIN."
        return
      }
    }

    BiometricAuthManager.authenticate(
      activity = fragmentActivity,
      title = "Unlock Space: ${space.name}",
      subtitle = "Verify your fingerprint or face to proceed",
      negativeButtonText = "Use Recovery PIN",
      cryptoObject = cryptoObject,
      onSuccess = { authResult ->
        val unlockedSpace = spaceViewModel.authenticateAndUnlockWithBiometric(space.id, authResult.cryptoObject)
        if (unlockedSpace != null) {
          successSpaceName = unlockedSpace.name
          coroutineScope.launch {
            delay(350)
            onUnlockSuccess(unlockedSpace)
          }
        } else {
          errorMessage = "Cryptographic verification failed. Enter Recovery PIN."
          inputMode = "RECOVERY_PIN"
        }
      },
      onError = { errorCode, errString ->
        if (errorCode == androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
          inputMode = "RECOVERY_PIN"
          errorMessage = null
        } else if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
          errorCode != androidx.biometric.BiometricPrompt.ERROR_CANCELED
        ) {
          errorMessage = errString.toString()
        }
      },
      onFailed = {
        errorMessage = "Biometric not recognized. Try again or enter Recovery PIN."
      }
    )
  }

  // Real-time clock state
  var currentTimeString by remember { mutableStateOf("") }
  var currentDateString by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    while (true) {
      val now = Date()
      currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
      currentDateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
      delay(1000)
    }
  }

  // Attempt biometric prompt once on start if space is biometric
  LaunchedEffect(targetSpace?.id) {
    if (targetSpace != null && (targetSpace.isBiometricProtected || targetSpace.authPolicy == Space.AUTH_BIOMETRIC)) {
      if (isBiometricAvailable && fragmentActivity != null && !isLockedOut) {
        delay(300)
        triggerBiometricPrompt()
      }
    }
  }

  fun attemptUnlock(credential: String) {
    if (isVerifying || credential.isBlank() || isLockedOut) return
    val space = targetSpace ?: return

    isVerifying = true
    errorMessage = null

    coroutineScope.launch {
      val matchedSpace = if (inputMode == "RECOVERY_PIN") {
        val unlocked = spaceViewModel.authenticateAndUnlockWithRecoveryPin(space.id, credential)
        if (unlocked != null && isKeyInvalidated) {
          // Key was invalidated, re-enroll automatically with recovery PIN
          spaceViewModel.reenrollBiometrics(space.id, credential)
        }
        unlocked
      } else {
        spaceViewModel.authenticateAndUnlockTargetSpace(space.id, credential)
      }

      isVerifying = false
      if (matchedSpace != null) {
        successSpaceName = matchedSpace.name
        delay(350)
        onUnlockSuccess(matchedSpace)
      } else {
        val remainingCooldown = spaceViewModel.getRemainingCooldownSeconds(space.id)
        errorMessage = if (remainingCooldown != null && remainingCooldown > 0) {
          "Too many attempts. Cooldown: ${remainingCooldown}s"
        } else {
          when (inputMode) {
            "RECOVERY_PIN" -> "Incorrect Recovery PIN. Try again."
            "PIN" -> "Incorrect PIN for '${space.name}'. Try again."
            else -> "Incorrect pattern for '${space.name}'. Try again."
          }
        }
        enteredPin = ""
        patternClearTrigger++
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .testTag("multi_space_lock_screen")
  ) {
    // 1. Wallpaper background
    val launcherLockBgType = targetSpace?.launcherLockWallpaperType ?: activeSpace?.launcherLockWallpaperType ?: Space.BACKGROUND_DEFAULT
    val launcherLockBgColor = targetSpace?.launcherLockWallpaperColor ?: activeSpace?.launcherLockWallpaperColor
    val launcherLockBgImageUri = targetSpace?.launcherLockWallpaperImageUri ?: activeSpace?.launcherLockWallpaperImageUri

    when {
      launcherLockBgType == Space.BACKGROUND_IMAGE || launcherLockBgType == Space.BACKGROUND_DEFAULT -> {
        val presetRes = WallpaperCatalog.resolveDrawableRes(launcherLockBgImageUri)
        if (presetRes != null) {
          Image(
            painter = painterResource(id = presetRes),
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (!launcherLockBgImageUri.isNullOrEmpty()) {
          AsyncImage(
            model = launcherLockBgImageUri,
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.img_wallpaper_aurora),
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        }
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
        )
      }
      launcherLockBgType == Space.BACKGROUND_COLOR && launcherLockBgColor != null -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(launcherLockBgColor.toInt()))
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
        )
      }
      else -> {
        Image(
          painter = painterResource(id = R.drawable.img_wallpaper_aurora),
          contentDescription = "Lock Screen Wallpaper",
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.50f))
        )
      }
    }

    // 2. Lock screen foreground content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header: Time, Date, Target Space Selector
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 6.dp)
        ) {
          Icon(
            imageVector = if (successSpaceName != null) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            tint = if (successSpaceName != null) Color(0xFF81C784) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = if (successSpaceName != null) "UNLOCKED" else "MULTI-SPACE SECURED",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = if (successSpaceName != null) Color(0xFF81C784) else Color.White.copy(alpha = 0.8f)
          )
        }

        Text(
          text = currentTimeString.ifEmpty { "00:00" },
          fontSize = 58.sp,
          fontWeight = FontWeight.ExtraLight,
          fontFamily = FontFamily.SansSerif,
          color = Color.White,
          modifier = Modifier.testTag("lock_screen_clock")
        )

        Text(
          text = currentDateString.ifEmpty { "Welcome" },
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.padding(top = 2.dp)
        )

        // Target Space Selector Chips (Explicit Space targeting)
        if (allSpaces.isNotEmpty()) {
          Text(
            text = "Select Space to unlock:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
          )

          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("lock_screen_target_space_selector")
          ) {
            items(allSpaces) { space ->
              val isSelected = targetSpace?.id == space.id
              val isBiometric = space.isBiometricProtected || space.authPolicy == Space.AUTH_BIOMETRIC
              val isPattern = space.isPatternProtected || space.authPolicy == Space.AUTH_PATTERN
              val isUnprotected = !space.isProtected

              Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (isSelected) Color.White.copy(alpha = 0.32f) else Color.White.copy(alpha = 0.12f),
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 0.5.dp,
                  color = if (isSelected) Color.White else Color.White.copy(alpha = 0.35f)
                ),
                modifier = Modifier
                  .clickable {
                    if (targetSpace?.id != space.id) {
                      selectedTargetSpaceId = space.id
                    }
                  }
                  .testTag("target_space_chip_${space.id}")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                  Icon(
                    imageVector = when {
                      isUnprotected -> Icons.Default.LockOpen
                      isBiometric -> Icons.Default.Fingerprint
                      isPattern -> Icons.Default.Gesture
                      else -> Icons.Default.Lock
                    },
                    contentDescription = null,
                    tint = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = space.name,
                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.75f),
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 12.sp
                  )
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Success or Error Feedback Banner
        AnimatedVisibility(
          visible = successSpaceName != null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut()
        ) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2E7D32).copy(alpha = 0.9f),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
              Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Text(
                text = "Welcome to '$successSpaceName'",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
            }
          }
        }

        AnimatedVisibility(
          visible = errorMessage != null && successSpaceName == null,
          enter = fadeIn() + expandVertically(),
          exit = fadeOut()
        ) {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFD32F2F).copy(alpha = 0.9f),
            modifier = Modifier.padding(vertical = 4.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
              Text(
                text = errorMessage ?: "",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Middle: Authentication Interface (PIN, Recovery PIN, Pattern, or Direct Access)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        val target = targetSpace
        val isTargetBiometric = target != null && (target.isBiometricProtected || target.authPolicy == Space.AUTH_BIOMETRIC)
        val isTargetPattern = target != null && (target.isPatternProtected || target.authPolicy == Space.AUTH_PATTERN)
        val isTargetUnprotected = target != null && !target.isProtected

        if (isTargetUnprotected) {
          // Unprotected space selected
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 24.dp)
          ) {
            Column(
              modifier = Modifier.padding(20.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
              Icon(Icons.Default.LockOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
              Text(
                text = "'${target?.name}' is not password protected.",
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
              )
              Button(
                onClick = {
                  if (target != null) {
                    spaceViewModel.selectActiveSpace(target.id)
                    spaceViewModel.unlockPhone()
                    onUnlockSuccess(target)
                  }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("btn_unlock_unprotected_space")
              ) {
                Text("Open '${target?.name}'", color = Color.Black, fontWeight = FontWeight.Bold)
              }
            }
          }
        } else if (inputMode == "PATTERN") {
          // Gesture Pattern Mode
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "Draw pattern for '${target?.name}'",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 8.dp)
              )

              PatternLockCanvas(
                rows = target?.patternRows ?: 3,
                cols = target?.patternCols ?: 3,
                isError = errorMessage != null,
                enabled = !isVerifying && !isLockedOut,
                clearTrigger = patternClearTrigger,
                onPatternStart = { errorMessage = null },
                onPatternComplete = { _, patternStr ->
                  attemptUnlock(patternStr)
                }
              )

              Spacer(modifier = Modifier.height(10.dp))

              TextButton(
                onClick = {
                  inputMode = "PIN"
                  errorMessage = null
                },
                modifier = Modifier.testTag("btn_switch_to_pin")
              ) {
                Icon(Icons.Default.Pin, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Use Numeric PIN", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              }
            }
          }
        } else {
          // PIN or RECOVERY_PIN Mode
          val isRecoveryMode = inputMode == "RECOVERY_PIN"

          Text(
            text = if (isRecoveryMode) "Enter Recovery PIN for '${target?.name}'" else "Enter PIN for '${target?.name}'",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isRecoveryMode) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.padding(bottom = 12.dp)
          )

          // PIN Indicator Dots
          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .padding(bottom = 16.dp)
              .testTag("lock_pin_dots_row")
          ) {
            val totalDots = 6
            for (i in 0 until totalDots) {
              val isFilled = i < enteredPin.length
              Box(
                modifier = Modifier
                  .size(13.dp)
                  .clip(CircleShape)
                  .background(
                    if (isFilled) (if (isRecoveryMode) Color(0xFFFFB74D) else Color.White) else Color.White.copy(alpha = 0.25f)
                  )
                  .border(
                    width = 1.dp,
                    color = if (isFilled) Color.White else Color.White.copy(alpha = 0.4f),
                    shape = CircleShape
                  )
              )
            }
          }

          // Numeric Keypad
          Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("lock_pin_numpad")
          ) {
            val keyRows = listOf(
              listOf("1", "2", "3"),
              listOf("4", "5", "6"),
              listOf("7", "8", "9"),
              listOf("switch", "0", "backspace")
            )

            keyRows.forEach { row ->
              Row(
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                row.forEach { key ->
                  when (key) {
                    "switch" -> {
                      Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                          .size(64.dp)
                          .clickable(
                            enabled = !isLockedOut,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                          ) {
                            if (isTargetPattern) {
                              inputMode = "PATTERN"
                            } else if (isTargetBiometric) {
                              inputMode = if (inputMode == "RECOVERY_PIN") "PIN" else "RECOVERY_PIN"
                            }
                            errorMessage = null
                          }
                          .testTag("btn_switch_auth_mode")
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = if (isTargetPattern) Icons.Default.Gesture else if (isRecoveryMode) Icons.Default.Lock else Icons.Default.VpnKey,
                            contentDescription = "Switch auth mode",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                          )
                        }
                      }
                    }
                    "backspace" -> {
                      Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                          .size(64.dp)
                          .clickable(
                            enabled = !isLockedOut && enteredPin.isNotEmpty(),
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                          ) {
                            if (enteredPin.isNotEmpty()) {
                              enteredPin = enteredPin.dropLast(1)
                              errorMessage = null
                            }
                          }
                          .testTag("btn_pin_backspace")
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Icon(
                            imageVector = Icons.Default.Backspace,
                            contentDescription = "Backspace",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                          )
                        }
                      }
                    }
                    else -> {
                      Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                          .size(64.dp)
                          .clickable(
                            enabled = !isLockedOut,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = true, color = Color.White)
                          ) {
                            if (enteredPin.length < 8) {
                              val newPin = enteredPin + key
                              enteredPin = newPin
                              errorMessage = null
                              if (newPin.length >= 4) {
                                attemptUnlock(newPin)
                              }
                            }
                          }
                          .testTag("btn_pin_digit_$key")
                      ) {
                        Box(contentAlignment = Alignment.Center) {
                          Text(
                            text = key,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                          )
                        }
                      }
                    }
                  }
                }
              }
            }

            if (isTargetBiometric && isBiometricAvailable) {
              Spacer(modifier = Modifier.height(4.dp))
              FilledTonalButton(
                onClick = { triggerBiometricPrompt() },
                enabled = !isLockedOut,
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = Color.White.copy(alpha = 0.22f),
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("btn_pin_biometric_unlock")
              ) {
                Icon(
                  imageVector = Icons.Default.Fingerprint,
                  contentDescription = "Unlock with Biometrics",
                  tint = Color.White,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Scan Biometrics",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 13.sp
                )
              }
            }
          }
        }
      }

      // Bottom Spacer
      Spacer(modifier = Modifier.height(8.dp))
    }
  }
}
