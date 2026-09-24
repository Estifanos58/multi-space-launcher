package com.multispace.domain.security

import kotlin.math.abs

/**
 * Handles pattern lock format canonicalization, parsing, validation, and intermediate node calculation.
 */
object PatternSecurityHelper {

  const val MIN_PATTERN_LENGTH = 4
  private const val PATTERN_PREFIX = "PATTERN:"

  /**
   * Greatest Common Divisor helper.
   */
  private fun gcd(a: Int, b: Int): Int {
    var x = abs(a)
    var y = abs(b)
    while (y != 0) {
      val t = y
      y = x % y
      x = t
    }
    return x
  }

  /**
   * Encodes a list of node indices into a canonical pattern string:
   * Example: "PATTERN:3x3:0-1-2-5-8"
   */
  fun encodePattern(rows: Int, cols: Int, nodes: List<Int>): String {
    return "$PATTERN_PREFIX${rows}x$cols:${nodes.joinToString("-")}"
  }

  fun isValidPatternFormat(patternString: String, expectedRows: Int = 3, expectedCols: Int = 3): Boolean {
    return parsePattern(patternString, expectedRows, expectedCols) != null
  }

  /**
   * Parses a pattern string into a list of node indices after validating format.
   * Returns null if the pattern string is invalid or malformed.
   */
  fun parsePattern(patternString: String, expectedRows: Int, expectedCols: Int): List<Int>? {
    if (!patternString.startsWith(PATTERN_PREFIX)) {
      // Legacy or unformatted pattern fallback check
      val parts = patternString.split("-", ",")
      if (parts.size >= MIN_PATTERN_LENGTH) {
        val parsed = parts.mapNotNull { it.trim().toIntOrNull() }
        val maxIndex = expectedRows * expectedCols - 1
        if (parsed.size == parts.size && parsed.all { it in 0..maxIndex } && parsed.distinct().size == parsed.size) {
          return parsed
        }
      }
      return null
    }

    val remainder = patternString.removePrefix(PATTERN_PREFIX)
    val tokens = remainder.split(":")
    if (tokens.size != 2) return null

    val dimToken = tokens[0]
    val nodesToken = tokens[1]

    val dimParts = dimToken.split("x")
    if (dimParts.size != 2) return null

    val rows = dimParts[0].toIntOrNull() ?: return null
    val cols = dimParts[1].toIntOrNull() ?: return null

    if (rows != expectedRows || cols != expectedCols) {
      return null
    }

    val nodeStrings = nodesToken.split("-")
    if (nodeStrings.size < MIN_PATTERN_LENGTH) {
      return null
    }

    val maxIndex = rows * cols - 1
    val parsedNodes = mutableListOf<Int>()
    val seen = mutableSetOf<Int>()

    for (nodeStr in nodeStrings) {
      val node = nodeStr.trim().toIntOrNull() ?: return null
      if (node !in 0..maxIndex) return null
      if (seen.contains(node)) return null // duplicate node
      seen.add(node)
      parsedNodes.add(node)
    }

    return parsedNodes
  }

  /**
   * Returns all unvisited intermediate nodes between [from] and [to] on an N x M grid.
   * For example, on a 3x3 grid, moving from 0 to 2 passes through 1.
   * If 1 is already visited, it is not returned.
   */
  fun getIntermediateNodes(
    from: Int,
    to: Int,
    rows: Int,
    cols: Int,
    visitedNodes: Set<Int>
  ): List<Int> {
    if (from == to) return emptyList()

    val r1 = from / cols
    val c1 = from % cols
    val r2 = to / cols
    val c2 = to % cols

    val dr = r2 - r1
    val dc = c2 - c1
    val g = gcd(dr, dc)

    if (g <= 1) return emptyList()

    val stepR = dr / g
    val stepC = dc / g

    val intermediates = mutableListOf<Int>()
    for (step in 1 until g) {
      val interR = r1 + step * stepR
      val interC = c1 + step * stepC
      val interNode = interR * cols + interC
      if (!visitedNodes.contains(interNode)) {
        intermediates.add(interNode)
      }
    }
    return intermediates
  }

  /**
   * Validates pattern complexity and minimum node count.
   */
  fun validatePattern(nodes: List<Int>, rows: Int, cols: Int): Result<Unit> {
    val totalNodes = rows * cols
    if (nodes.size < MIN_PATTERN_LENGTH) {
      return Result.failure(IllegalArgumentException("Pattern must connect at least $MIN_PATTERN_LENGTH dots."))
    }
    if (nodes.any { it < 0 || it >= totalNodes }) {
      return Result.failure(IllegalArgumentException("Pattern contains invalid node coordinates."))
    }
    if (nodes.distinct().size != nodes.size) {
      return Result.failure(IllegalArgumentException("Pattern dots cannot be visited more than once."))
    }
    return Result.success(Unit)
  }
}
