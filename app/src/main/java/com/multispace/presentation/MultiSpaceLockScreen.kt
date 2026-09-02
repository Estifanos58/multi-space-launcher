package com.multispace.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.multispace.ui.theme.PrimaryContainerLight
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MultiSpaceLockScreen(
  spaceViewModel: SpaceViewModel,
  onUnlockSuccess: (Space) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val allSpaces by spaceViewModel.allSpaces.collectAsState()
  val activeSpace by spaceViewModel.activeSpace.collectAsState()

  var inputMode by remember { mutableStateOf("PIN") } // "PIN" or "PATTERN"
  var enteredPin by remember { mutableStateOf("") }
  var isVerifying by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var successSpaceName by remember { mutableStateOf<String?>(null) }
  var patternClearTrigger by remember { mutableIntStateOf(0) }

  val coroutineScope = rememberCoroutineScope()
  val fragmentActivity = remember(context) { BiometricAuthManager.findFragmentActivity(context) }
  val isBiometricAvailable = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }

  fun triggerBiometricPrompt() {
    if (fragmentActivity == null || !isBiometricAvailable) return
    BiometricAuthManager.authenticate(
      activity = fragmentActivity,
      title = "Unlock Multi-Space",
      subtitle = "Verify your fingerprint or face to access your Space",
      negativeButtonText = "Use PIN / Pattern",
      onSuccess = {
        val targetSpace = spaceViewModel.authenticateAndUnlockWithBiometric()
        if (targetSpace != null) {
          successSpaceName = targetSpace.name
          coroutineScope.launch {
            delay(350)
            onUnlockSuccess(targetSpace)
          }
        }
      },
      onError = { errorCode, errString ->
        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
          errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
          errorCode != androidx.biometric.BiometricPrompt.ERROR_CANCELED
        ) {
          errorMessage = errString.toString()
        }
      },
      onFailed = {
        errorMessage = "Biometric not recognized. Try again."
      }
    )
  }

  // Real-time clock state
  var currentTimeString by remember { mutableStateOf("") }
  var currentDateString by remember { mutableStateOf("") }

  LaunchedEffect(Unit) {
    // Attempt biometric prompt once when screen is first presented if available
    if (isBiometricAvailable && fragmentActivity != null) {
      delay(300)
      triggerBiometricPrompt()
    }
  }

  LaunchedEffect(Unit) {
    while (true) {
      val now = Date()
      currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(now)
      currentDateString = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(now)
      delay(1000)
    }
  }

  // Check if any pattern-protected space exists to auto-recommend pattern toggle
  val hasPatternSpaces = remember(allSpaces) {
    allSpaces.any { it.isPatternProtected || it.authPolicy == Space.AUTH_PATTERN }
  }
  val unprotectedSpace = remember(allSpaces) {
    allSpaces.firstOrNull { !it.isProtected }
  }

  fun attemptUnlock(credential: String) {
    if (isVerifying || credential.isBlank()) return
    isVerifying = true
    errorMessage = null

    coroutineScope.launch {
      val matchedSpace = spaceViewModel.authenticateAndUnlockSpaceByCredential(credential)
      isVerifying = false
      if (matchedSpace != null) {
        successSpaceName = matchedSpace.name
        delay(350)
        onUnlockSuccess(matchedSpace)
      } else {
        errorMessage = if (inputMode == "PIN") "Incorrect PIN. No matching space found." else "Incorrect pattern. No matching space found."
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
    val phoneLockBgType = activeSpace?.phoneLockWallpaperType ?: Space.BACKGROUND_DEFAULT
    val phoneLockBgColor = activeSpace?.phoneLockWallpaperColor
    val phoneLockBgImageUri = activeSpace?.phoneLockWallpaperImageUri

    when {
      phoneLockBgType == Space.BACKGROUND_IMAGE || phoneLockBgType == Space.BACKGROUND_DEFAULT -> {
        val presetRes = WallpaperCatalog.resolveDrawableRes(phoneLockBgImageUri)
        if (presetRes != null) {
          Image(
            painter = painterResource(id = presetRes),
            contentDescription = "Lock Screen Wallpaper",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else if (!phoneLockBgImageUri.isNullOrEmpty()) {
          AsyncImage(
            model = phoneLockBgImageUri,
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
      phoneLockBgType == Space.BACKGROUND_COLOR && phoneLockBgColor != null -> {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color(phoneLockBgColor))
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
        .padding(horizontal = 24.dp, vertical = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.SpaceBetween
    ) {
      // Header: Time, Date, Status
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.padding(bottom = 8.dp)
        ) {
          Icon(
            imageVector = if (successSpaceName != null) Icons.Default.LockOpen else Icons.Default.Lock,
            contentDescription = null,
            tint = if (successSpaceName != null) Color(0xFF81C784) else Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(18.dp)
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
          fontSize = 64.sp,
          fontWeight = FontWeight.ExtraLight,
          fontFamily = FontFamily.SansSerif,
          color = Color.White,
          modifier = Modifier.testTag("lock_screen_clock")
        )

        Text(
          text = currentDateString.ifEmpty { "Welcome" },
          fontSize = 15.sp,
          fontWeight = FontWeight.Medium,
          color = Color.White.copy(alpha = 0.85f),
          modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

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

        if (errorMessage == null && successSpaceName == null) {
          Text(
            text = "Enter any Space credential to jump directly into it",
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
          )
        }
      }

      // Middle: Authentication Interface (PIN Numpad or Gesture Pattern)
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
      ) {
        if (inputMode == "PIN") {
          // PIN Indicator Dots
          Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .padding(bottom = 20.dp)
              .testTag("lock_pin_dots_row")
          ) {
            val totalDots = 6
            for (i in 0 until totalDots) {
              val isFilled = i < enteredPin.length
              Box(
                modifier = Modifier
                  .size(14.dp)
                  .clip(CircleShape)
                  .background(
                    if (isFilled) Color.White else Color.White.copy(alpha = 0.25f)
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.testTag("lock_pin_numpad")
          ) {
            val keyRows = listOf(
              listOf("1", "2", "3"),
              listOf("4", "5", "6"),
              listOf("7", "8", "9"),
              listOf("mode", "0", "backspace")
            )

            keyRows.forEach { row ->
              Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                row.forEach { key ->
                  when (key) {
                    "mode" -> {
                      if (hasPatternSpaces) {
                        Surface(
                          shape = CircleShape,
                          color = Color.White.copy(alpha = 0.12f),
                          modifier = Modifier
                            .size(68.dp)
                            .clickable(
                              interactionSource = remember { MutableInteractionSource() },
                              indication = ripple(bounded = true, color = Color.White)
                            ) {
                              inputMode = "PATTERN"
                              errorMessage = null
                            }
                            .testTag("btn_switch_to_pattern")
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Icon(
                              imageVector = Icons.Default.Gesture,
                              contentDescription = "Switch to Pattern Mode",
                              tint = Color.White,
                              modifier = Modifier.size(24.dp)
                            )
                          }
                        }
                      } else {
                        // Clear button
                        Surface(
                          shape = CircleShape,
                          color = Color.White.copy(alpha = 0.08f),
                          modifier = Modifier
                            .size(68.dp)
                            .clickable(
                              interactionSource = remember { MutableInteractionSource() },
                              indication = ripple(bounded = true, color = Color.White)
                            ) {
                              enteredPin = ""
                              errorMessage = null
                            }
                            .testTag("btn_pin_clear")
                        ) {
                          Box(contentAlignment = Alignment.Center) {
                            Text(
                              text = "C",
                              fontSize = 18.sp,
                              fontWeight = FontWeight.Bold,
                              color = Color.White.copy(alpha = 0.8f)
                            )
                          }
                        }
                      }
                    }
                    "backspace" -> {
                      Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier
                          .size(68.dp)
                          .clickable(
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
                            modifier = Modifier.size(22.dp)
                          )
                        }
                      }
                    }
                    else -> {
                      Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                        modifier = Modifier
                          .size(68.dp)
                          .clickable(
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
                            fontSize = 26.sp,
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

            if (isBiometricAvailable) {
              Spacer(modifier = Modifier.height(4.dp))
              FilledTonalButton(
                onClick = { triggerBiometricPrompt() },
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
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Unlock with Fingerprint",
                  fontWeight = FontWeight.SemiBold,
                  fontSize = 14.sp
                )
              }
            }
          }
        } else {
          // Gesture Pattern Mode
          Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.Black.copy(alpha = 0.45f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 8.dp)
          ) {
            Column(
              modifier = Modifier.padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              PatternLockCanvas(
                rows = 3,
                cols = 3,
                isError = errorMessage != null,
                enabled = !isVerifying,
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
                Text("Use Numeric PIN instead", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              }
              if (isBiometricAvailable) {
                Spacer(modifier = Modifier.height(12.dp))
                FilledTonalButton(
                  onClick = { triggerBiometricPrompt() },
                  colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color.White.copy(alpha = 0.22f),
                    contentColor = Color.White
                  ),
                  shape = RoundedCornerShape(20.dp),
                  modifier = Modifier.testTag("btn_lock_screen_biometric")
                ) {
                  Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock with Biometrics",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(
                    text = "Unlock with Fingerprint",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                  )
                }
              }
            }
          }
        }
      }

      // Bottom Bar: Fallback to Guest / Unprotected Space if available
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        if (unprotectedSpace != null) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.12f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
            modifier = Modifier
              .clickable {
                spaceViewModel.selectActiveSpace(unprotectedSpace.id)
                spaceViewModel.unlockPhone()
                onUnlockSuccess(unprotectedSpace)
              }
              .testTag("btn_open_guest_space")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
              Icon(Icons.Default.PersonOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
              Text(
                text = "Open '${unprotectedSpace.name}' (No PIN)",
                color = Color.White,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp
              )
            }
          }
        }
      }
    }
  }
}
