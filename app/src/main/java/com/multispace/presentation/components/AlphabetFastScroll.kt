package com.multispace.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.multispace.ui.theme.QuantumViolet
import kotlin.math.roundToInt

/**
 * A fast-scrolling alphabetical index (A–Z) displayed along the rightmost edge of Layer 2
 * in Vertical mode.
 *
 * Supports single-touch, press, and continuous dragging across letters, updating the
 * scroll position of the app list to the first app starting with that letter.
 *
 * Letters with no matching apps have a visually distinct disabled/inactive appearance.
 */
@Composable
fun AlphabetFastScroll(
  alphabet: List<Char>,
  activeLetters: Set<Char>,
  onLetterSelected: (Char) -> Unit,
  modifier: Modifier = Modifier
) {
  var isDragging by remember { mutableStateOf(false) }
  var currentLetter by remember { mutableStateOf<Char?>(null) }
  var dragY by remember { mutableFloatStateOf(0f) }
  var totalHeightPx by remember { mutableFloatStateOf(0f) }

  val density = LocalDensity.current

  Box(
    modifier = modifier
      .width(26.dp)
      .wrapContentHeight()
      .testTag("layer2_alphabet_fast_scroll"),
    contentAlignment = Alignment.CenterEnd
  ) {
    // 1. Vertical Alphabet Column Strip (Translucent container pinned to the right edge)
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
      border = BorderStroke(
        0.8.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.40f)
      ),
      shadowElevation = 0.dp,
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .width(18.dp)
        .height(350.dp)
        .onGloballyPositioned { coords ->
          totalHeightPx = coords.size.height.toFloat()
        }
        .pointerInput(alphabet, activeLetters) {
          awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            val height = size.height.toFloat()
            if (height > 0f) {
              val initialFraction = (down.position.y / height).coerceIn(0f, 0.999f)
              val index = (initialFraction * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
              val letter = alphabet[index]
              isDragging = true
              currentLetter = letter
              dragY = down.position.y
              onLetterSelected(letter)
              down.consume()
            }

            while (true) {
              val event = awaitPointerEvent()
              val change = event.changes.firstOrNull() ?: break
              if (!change.pressed) break

              val y = change.position.y
              dragY = y
              val fraction = (y / height).coerceIn(0f, 0.999f)
              val index = (fraction * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
              val letter = alphabet[index]
              if (letter != currentLetter) {
                currentLetter = letter
                onLetterSelected(letter)
              }
              change.consume()
            }

            isDragging = false
            currentLetter = null
          }
        }
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        alphabet.forEach { letter ->
          val isSelected = isDragging && currentLetter == letter

          Box(
            modifier = Modifier
              .weight(1f)
              .fillMaxWidth()
              .testTag("layer2_alphabet_letter_$letter"),
            contentAlignment = Alignment.Center
          ) {
            if (isSelected) {
              Box(
                modifier = Modifier
                  .size(12.dp)
                  .background(MaterialTheme.colorScheme.primary, CircleShape)
              )
            }
            Text(
              text = letter.toString(),
              fontSize = 8.5.sp,
              fontWeight = FontWeight.Bold,
              color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
              } else {
                MaterialTheme.colorScheme.onSurface
              },
              textAlign = TextAlign.Center
            )
          }
        }
      }
    }

    // 2. Compact Floating Letter Preview Bubble positioned to the left of the strip
    AnimatedVisibility(
      visible = isDragging && currentLetter != null,
      enter = fadeIn() + scaleIn(),
      exit = fadeOut() + scaleOut(),
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset {
          val bubbleSizePx = with(density) { 44.dp.toPx() }
          val bubbleOffsetXPx = with(density) { (-48).dp.toPx() }
          val clampedY = (dragY - bubbleSizePx / 2f).coerceIn(0f, (totalHeightPx - bubbleSizePx).coerceAtLeast(0f))
          IntOffset(bubbleOffsetXPx.roundToInt(), clampedY.roundToInt())
        }
    ) {
      val letter = currentLetter ?: 'A'

      Surface(
        shape = CircleShape,
        color = QuantumViolet,
        shadowElevation = 6.dp,
        border = BorderStroke(
          1.dp,
          Color.White.copy(alpha = 0.35f)
        ),
        modifier = Modifier
          .size(44.dp)
          .testTag("layer2_alphabet_bubble")
      ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
          Text(
            text = letter.toString(),
            style = MaterialTheme.typography.titleLarge.copy(
              fontSize = 20.sp,
              fontWeight = FontWeight.Black
            ),
            color = Color.White,
            textAlign = TextAlign.Center
          )
        }
      }
    }
  }
}
