package com.multispace.domain.model

/**
 * Encapsulates the results of an attempt to import layout information from the current Android system.
 *
 * @property sourceLauncherPackage Package name of the detected default home launcher.
 * @property sourceLauncherLabel Human-readable label of the detected default home launcher.
 * @property successItems List of successfully detected and imported configuration attributes.
 * @property partiallyImportedItems List of attributes that were approximated or partially imported.
 * @property restrictedItems List of attributes unavailable due to Android sandbox security restrictions.
 * @property summary General status message.
 */
data class ImportReport(
  val sourceLauncherPackage: String?,
  val sourceLauncherLabel: String?,
  val successItems: List<String> = emptyList(),
  val partiallyImportedItems: List<String> = emptyList(),
  val restrictedItems: List<String> = emptyList(),
  val summary: String
)
