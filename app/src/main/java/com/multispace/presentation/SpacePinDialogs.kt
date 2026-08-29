package com.multispace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.domain.model.Space
import com.multispace.platform.PinSecurityManager
import com.multispace.ui.theme.LightSurfaceContainerHigh
import com.multispace.ui.theme.LightSurfaceContainerLowest
import com.multispace.ui.theme.PrimaryContainerLight
import com.multispace.ui.theme.PrimaryPurple
import com.multispace.ui.theme.PrimaryPurpleDark
import com.multispace.ui.theme.TextMuted
import com.multispace.ui.theme.TextPrimary
import com.multispace.ui.theme.TextSecondary
import kotlinx.coroutines.launch

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

@Composable
fun SpaceUnlockDialog(
  space: Space,
  onDismiss: () -> Unit,
  onUnlockSuccess: () -> Unit,
  spaceViewModel: SpaceViewModel
) {
  val isPatternAuth = space.isPatternProtected || space.authPolicy == Space.AUTH_PATTERN
  var enteredPin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  var isVerifying by remember { mutableStateOf(false) }
  var clearTrigger by remember { mutableIntStateOf(0) }
  val coroutineScope = rememberCoroutineScope()

  AlertDialog(
    onDismissRequest = {
      if (!isVerifying) onDismiss()
    },
    icon = {
      Surface(
        shape = CircleShape,
        color = PrimaryContainerLight,
        modifier = Modifier.size(54.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = if (isPatternAuth) Icons.Default.Gesture else Icons.Default.Lock,
            contentDescription = null,
            tint = PrimaryPurpleDark,
            modifier = Modifier.size(28.dp)
          )
        }
      }
    },
    title = {
      Text(
        text = "Unlock '${space.name}'",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text(
          text = if (isPatternAuth) "Draw gesture pattern to access this Space." else "Enter numeric PIN to access this Space.",
          style = MaterialTheme.typography.bodySmall,
          color = TextSecondary,
          textAlign = TextAlign.Center
        )

        if (isPatternAuth) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = LightSurfaceContainerLowest,
            border = androidx.compose.foundation.BorderStroke(1.dp, LightSurfaceContainerHigh),
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
          ) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
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
                    val success = spaceViewModel.verifyAndUnlockSpace(space.id, patternStr)
                    isVerifying = false
                    if (success) {
                      onUnlockSuccess()
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
            label = { Text("PIN") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true,
            isError = isError,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("input_unlock_pin")
          )
        }

        if (isError) {
          Text(
            text = if (isPatternAuth) "Incorrect pattern. Please try again." else "Incorrect PIN. Please try again.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFC62828),
            fontWeight = FontWeight.Medium
          )
        }

        if (isVerifying) {
          CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            color = PrimaryPurpleDark,
            strokeWidth = 2.dp
          )
        }
      }
    },
    confirmButton = {
      if (!isPatternAuth) {
        Button(
          onClick = {
            if (enteredPin.isBlank()) {
              isError = true
              return@Button
            }
            isVerifying = true
            coroutineScope.launch {
              val success = spaceViewModel.verifyAndUnlockSpace(space.id, enteredPin)
              isVerifying = false
              if (success) {
                onUnlockSuccess()
              } else {
                isError = true
                enteredPin = ""
              }
            }
          },
          enabled = !isVerifying && enteredPin.length >= 4,
          colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
          modifier = Modifier.testTag("btn_submit_unlock_pin")
        ) {
          Text("Unlock")
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
