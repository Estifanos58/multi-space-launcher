package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.Space
import com.multispace.platform.BiometricAuthManager
import com.multispace.platform.PinSecurityManager
import com.multispace.ui.theme.*
import kotlinx.coroutines.launch

private val dialogTextFieldColors
  @Composable get() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedContainerColor = LightSurfaceContainerLowest,
    unfocusedContainerColor = LightSurfaceContainerLowest,
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = Color(0xFFCAC4D0),
    focusedLabelColor = PrimaryPurple,
    unfocusedLabelColor = TextSecondary,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted,
    cursorColor = PrimaryPurple
  )

@Composable
fun SetSpacePinDialog(
  space: Space,
  onDismiss: () -> Unit,
  onPinSet: (pin: String) -> Unit
) {
  var pin by remember { mutableStateOf("") }
  var confirmPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showPin by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Surface(
        shape = CircleShape,
        color = PrimaryContainerLight,
        modifier = Modifier.size(48.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = PrimaryPurpleDark,
            modifier = Modifier.size(24.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "Set PIN for '${space.name}'",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text(
          text = "Protect this Space with a 4 to 8 digit numeric PIN. The PIN is required when opening or switching to this Space.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )

        OutlinedTextField(
          value = pin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              pin = input
              errorMessage = null
            }
          },
          label = { Text("Enter PIN (4-8 digits)") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          trailingIcon = {
            IconButton(onClick = { showPin = !showPin }) {
              Icon(
                imageVector = if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                contentDescription = if (showPin) "Hide PIN" else "Show PIN",
                tint = TextSecondary
              )
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_set_pin")
        )

        OutlinedTextField(
          value = confirmPin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              confirmPin = input
              errorMessage = null
            }
          },
          label = { Text("Confirm PIN") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_confirm_pin")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828),
            fontWeight = FontWeight.Medium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!PinSecurityManager.isValidPinFormat(pin)) {
            errorMessage = "PIN must be between 4 and 8 digits."
          } else if (pin != confirmPin) {
            errorMessage = "PINs do not match. Please re-enter."
          } else {
            onPinSet(pin)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        modifier = Modifier.testTag("btn_save_pin")
      ) {
        Text("Enable PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun ChangeSpacePinDialog(
  space: Space,
  onDismiss: () -> Unit,
  onPinChanged: (currentPin: String, newPin: String) -> Unit
) {
  var currentPin by remember { mutableStateOf("") }
  var newPin by remember { mutableStateOf("") }
  var confirmNewPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showPin by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.LockReset,
        contentDescription = null,
        tint = PrimaryPurpleDark,
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Change PIN for '${space.name}'",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        OutlinedTextField(
          value = currentPin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              currentPin = input
              errorMessage = null
            }
          },
          label = { Text("Current PIN") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_current_pin")
        )

        OutlinedTextField(
          value = newPin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              newPin = input
              errorMessage = null
            }
          },
          label = { Text("New PIN (4-8 digits)") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_new_pin")
        )

        OutlinedTextField(
          value = confirmNewPin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              confirmNewPin = input
              errorMessage = null
            }
          },
          label = { Text("Confirm New PIN") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_confirm_new_pin")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828),
            fontWeight = FontWeight.Medium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (currentPin.isBlank()) {
            errorMessage = "Please enter your current PIN."
          } else if (!PinSecurityManager.isValidPinFormat(newPin)) {
            errorMessage = "New PIN must be between 4 and 8 digits."
          } else if (newPin != confirmNewPin) {
            errorMessage = "New PINs do not match."
          } else {
            onPinChanged(currentPin, newPin)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
        modifier = Modifier.testTag("btn_confirm_change_pin")
      ) {
        Text("Update PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun DisableSpacePinDialog(
  space: Space,
  onDismiss: () -> Unit,
  onPinDisabled: (currentPin: String) -> Unit
) {
  var currentPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.LockOpen,
        contentDescription = null,
        tint = Color(0xFFC62828),
        modifier = Modifier.size(32.dp)
      )
    },
    title = {
      Text(
        text = "Disable PIN for '${space.name}'",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text(
          text = "Enter your current PIN to remove protection. Anyone will be able to access this Space without authentication.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary
        )

        OutlinedTextField(
          value = currentPin,
          onValueChange = { input ->
            if (input.all { it.isDigit() } && input.length <= 8) {
              currentPin = input
              errorMessage = null
            }
          },
          label = { Text("Current PIN") },
          textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
          shape = RoundedCornerShape(12.dp),
          colors = dialogTextFieldColors,
          visualTransformation = PasswordVisualTransformation(),
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("input_disable_pin")
        )

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828),
            fontWeight = FontWeight.Medium
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (currentPin.isBlank()) {
            errorMessage = "Please enter your current PIN."
          } else {
            onPinDisabled(currentPin)
          }
        },
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
        modifier = Modifier.testTag("btn_confirm_disable_pin")
      ) {
        Text("Disable PIN")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

enum class AuthDialogMode {
  UNLOCK,
  EDIT,
  DELETE
}

@Composable
fun SpaceCredentialVerificationDialog(
  space: Space,
  mode: AuthDialogMode,
  onDismiss: () -> Unit,
  onSuccess: () -> Unit,
  spaceViewModel: SpaceViewModel
) {
  val context = LocalContext.current
  val fragmentActivity = remember(context) { BiometricAuthManager.findFragmentActivity(context) }
  val isBiometricAvailable = remember(context) { BiometricAuthManager.isBiometricAvailable(context) }

  val isPatternAuth = space.isPatternProtected || space.authPolicy == Space.AUTH_PATTERN
  val isBiometricAuth = space.isBiometricProtected || space.authPolicy == Space.AUTH_BIOMETRIC
  var enteredPin by remember { mutableStateOf("") }
  var showPinText by remember { mutableStateOf(false) }
  var isError by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var clearTrigger by remember { mutableIntStateOf(0) }
  val coroutineScope = rememberCoroutineScope()

  fun triggerBiometricPrompt() {
    if (fragmentActivity == null || !isBiometricAvailable) return
    isVerifying = true
    isError = false
    BiometricAuthManager.authenticate(
      activity = fragmentActivity,
      title = "Authorize '${space.name}'",
      subtitle = "Scan fingerprint or face to proceed",
      negativeButtonText = if (isBiometricAuth) "Cancel" else "Use PIN / Pattern",
      onSuccess = {
        isVerifying = false
        if (mode == AuthDialogMode.UNLOCK) {
          spaceViewModel.unlockSpace(space.id)
        }
        onSuccess()
      },
      onError = { errorCode, _ ->
        isVerifying = false
        if (errorCode != androidx.biometric.BiometricPrompt.ERROR_USER_CANCELED &&
          errorCode != androidx.biometric.BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
          errorCode != androidx.biometric.BiometricPrompt.ERROR_CANCELED
        ) {
          isError = true
        }
      },
      onFailed = {
        isVerifying = false
        isError = true
      }
    )
  }

  LaunchedEffect(Unit) {
    if (isBiometricAuth && isBiometricAvailable && fragmentActivity != null) {
      triggerBiometricPrompt()
    }
  }

  val iconContainerBg: Color
  val iconTint: Color
  val primaryButtonColor: Color
  val dialogTitle: String
  val dialogSubtitle: String
  val confirmButtonText: String
  val headerIcon: ImageVector

  when (mode) {
    AuthDialogMode.UNLOCK -> {
      iconContainerBg = PrimaryContainerLight
      iconTint = PrimaryPurpleDark
      primaryButtonColor = PrimaryPurple
      headerIcon = when {
        isBiometricAuth -> Icons.Default.Fingerprint
        isPatternAuth -> Icons.Default.Gesture
        else -> Icons.Default.Lock
      }
      dialogTitle = "Unlock '${space.name}'"
      dialogSubtitle = when {
        isBiometricAuth -> "Authenticate with your fingerprint or face to access '${space.name}'."
        isPatternAuth -> "Draw your pattern gesture to access '${space.name}'."
        else -> "Enter your numeric PIN to access '${space.name}'."
      }
      confirmButtonText = "Unlock Space"
    }
    AuthDialogMode.EDIT -> {
      iconContainerBg = Color(0xFFE3F2FD)
      iconTint = Color(0xFF1565C0)
      primaryButtonColor = Color(0xFF1565C0)
      headerIcon = when {
        isBiometricAuth -> Icons.Default.Fingerprint
        isPatternAuth -> Icons.Default.Gesture
        else -> Icons.Default.Lock
      }
      dialogTitle = "Unlock to Edit '${space.name}'"
      dialogSubtitle = when {
        isBiometricAuth -> "Authenticate with biometrics to edit '${space.name}'."
        isPatternAuth -> "Draw your pattern gesture to edit settings and apps for '${space.name}'."
        else -> "Enter your PIN to edit settings and apps for '${space.name}'."
      }
      confirmButtonText = "Verify & Edit"
    }
    AuthDialogMode.DELETE -> {
      iconContainerBg = Color(0xFFFFEBEE)
      iconTint = Color(0xFFC62828)
      primaryButtonColor = Color(0xFFC62828)
      headerIcon = when {
        isBiometricAuth -> Icons.Default.Fingerprint
        isPatternAuth -> Icons.Default.Gesture
        else -> Icons.Default.DeleteOutline
      }
      dialogTitle = "Authorize Deletion of '${space.name}'"
      dialogSubtitle = when {
        isBiometricAuth -> "Space '${space.name}' is biometric protected. Verify fingerprint to confirm deletion."
        isPatternAuth -> "Space '${space.name}' is secured with a pattern. Draw pattern to confirm deletion."
        else -> "Space '${space.name}' is secured with a PIN. Enter PIN to confirm deletion."
      }
      confirmButtonText = "Delete Space"
    }
  }

  AlertDialog(
    onDismissRequest = {
      if (!isVerifying) onDismiss()
    },
    icon = {
      Surface(
        shape = CircleShape,
        color = iconContainerBg,
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = headerIcon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = dialogTitle,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        color = TextPrimary
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = dialogSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          textAlign = TextAlign.Center
        )

        if (isBiometricAuth) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LightSurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightSurfaceContainerHigh),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 8.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Fingerprint,
                contentDescription = "Biometric Sensor",
                tint = primaryButtonColor,
                modifier = Modifier.size(48.dp)
              )
              Text(
                text = "Biometric Lock Active",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = TextPrimary
              )
              Button(
                onClick = { triggerBiometricPrompt() },
                colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("btn_verify_biometric")
              ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scan Fingerprint")
              }
            }
          }
        } else if (isPatternAuth) {
          Surface(
            shape = RoundedCornerShape(20.dp),
            color = LightSurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(
              width = if (isError) 1.5.dp else 1.dp,
              color = if (isError) Color(0xFFC62828) else LightSurfaceContainerHigh
            ),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              contentAlignment = Alignment.Center
            ) {
              PatternLockCanvas(
                rows = space.patternRows,
                cols = space.patternCols,
                isError = isError,
                enabled = !isVerifying,
                clearTrigger = clearTrigger,
                onPatternStart = { isError = false },
                onPatternComplete = { _, patternStr ->
                  isVerifying = true
                  coroutineScope.launch {
                    val isValid = if (mode == AuthDialogMode.DELETE) {
                      spaceViewModel.spaceRepository.verifySpacePin(space.id, patternStr)
                    } else {
                      spaceViewModel.verifyAndUnlockSpace(space.id, patternStr)
                    }
                    isVerifying = false
                    if (isValid) {
                      onSuccess()
                    } else {
                      isError = true
                      clearTrigger++
                    }
                  }
                }
              )
            }
          }
        } else {
          OutlinedTextField(
            value = enteredPin,
            onValueChange = { input ->
              if (input.all { it.isDigit() } && input.length <= 8) {
                enteredPin = input
                isError = false
              }
            },
            label = { Text("PIN (4-8 digits)") },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold),
            shape = RoundedCornerShape(12.dp),
            colors = dialogTextFieldColors,
            visualTransformation = if (showPinText) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = isError,
            trailingIcon = {
              IconButton(onClick = { showPinText = !showPinText }) {
                Icon(
                  imageVector = if (showPinText) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = "Toggle PIN visibility",
                  tint = TextSecondary
                )
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag(if (mode == AuthDialogMode.DELETE) "input_delete_confirm_pin" else "input_unlock_pin")
          )
        }

        if (!isBiometricAuth && isBiometricAvailable) {
          TextButton(
            onClick = { triggerBiometricPrompt() },
            modifier = Modifier.testTag("btn_quick_biometric_unlock")
          ) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = primaryButtonColor,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Or verify with Fingerprint",
              color = primaryButtonColor,
              fontWeight = FontWeight.SemiBold,
              fontSize = 13.sp
            )
          }
        }

        if (isError) {
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFFFFEBEE),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = when {
                  isBiometricAuth -> "Biometric verification failed. Please try again."
                  isPatternAuth -> "Incorrect pattern. Please try again."
                  else -> "Incorrect PIN. Please try again."
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFC62828),
                fontWeight = FontWeight.Medium
              )
            }
          }
        }

        if (isVerifying) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp)
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(18.dp),
              color = primaryButtonColor,
              strokeWidth = 2.dp
            )
            Text(
              text = "Verifying security credential...",
              fontSize = 12.sp,
              color = TextSecondary
            )
          }
        }
      }
    },
    confirmButton = {
      if (!isPatternAuth && !isBiometricAuth) {
        Button(
          onClick = {
            if (enteredPin.isBlank()) {
              isError = true
              return@Button
            }
            isVerifying = true
            coroutineScope.launch {
              val isValid = if (mode == AuthDialogMode.DELETE) {
                spaceViewModel.spaceRepository.verifySpacePin(space.id, enteredPin)
              } else {
                spaceViewModel.verifyAndUnlockSpace(space.id, enteredPin)
              }
              isVerifying = false
              if (isValid) {
                onSuccess()
              } else {
                isError = true
                enteredPin = ""
              }
            }
          },
          enabled = !isVerifying && enteredPin.length >= 4,
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
          modifier = Modifier.testTag(if (mode == AuthDialogMode.DELETE) "btn_confirm_delete_protected" else "btn_submit_unlock_pin")
        ) {
          Text(confirmButtonText, fontWeight = FontWeight.Bold)
        }
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isVerifying
      ) {
        Text("Cancel")
      }
    }
  )
}

@Composable
fun SpaceUnlockDialog(
  space: Space,
  onDismiss: () -> Unit,
  onUnlockSuccess: () -> Unit,
  spaceViewModel: SpaceViewModel
) {
  SpaceCredentialVerificationDialog(
    space = space,
    mode = AuthDialogMode.UNLOCK,
    onDismiss = onDismiss,
    onSuccess = onUnlockSuccess,
    spaceViewModel = spaceViewModel
  )
}

@Composable
fun EditSpaceCredentialDialog(
  space: Space,
  onDismiss: () -> Unit,
  onAuthSuccess: () -> Unit,
  spaceViewModel: SpaceViewModel
) {
  SpaceCredentialVerificationDialog(
    space = space,
    mode = AuthDialogMode.EDIT,
    onDismiss = onDismiss,
    onSuccess = onAuthSuccess,
    spaceViewModel = spaceViewModel
  )
}

@Composable
fun DeleteSpaceCredentialDialog(
  space: Space,
  onDismiss: () -> Unit,
  onConfirmDelete: () -> Unit,
  spaceViewModel: SpaceViewModel
) {
  SpaceCredentialVerificationDialog(
    space = space,
    mode = AuthDialogMode.DELETE,
    onDismiss = onDismiss,
    onSuccess = onConfirmDelete,
    spaceViewModel = spaceViewModel
  )
}
