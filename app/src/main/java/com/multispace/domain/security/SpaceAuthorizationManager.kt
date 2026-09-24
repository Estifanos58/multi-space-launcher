package com.multispace.domain.security

import com.multispace.domain.model.Space

/**
 * Context provided when evaluating sensitive authorization requests.
 */
data class AuthorizationContext(
  val hasExplicitAuthentication: Boolean = false,
  val callerToken: String? = null
) {
  companion object {
    val Default = AuthorizationContext()
    val Explicit = AuthorizationContext(hasExplicitAuthentication = true)
  }
}

/**
 * Detailed status code for authorization decisions.
 */
enum class AuthorizationStatus {
  AUTHORIZED,
  DENIED_LAUNCHER_LOCKED,
  DENIED_SESSION_LOCKED,
  DENIED_REQUIRES_EXPLICIT_AUTH,
  DENIED_NOT_CONFIGURED,
  DENIED_UNKNOWN_POLICY
}

/**
 * Result returned by [SpaceAuthorizationManager.authorize].
 */
data class AuthorizationResult(
  val isAuthorized: Boolean,
  val status: AuthorizationStatus,
  val message: String? = null
) {
  companion object {
    val Authorized = AuthorizationResult(true, AuthorizationStatus.AUTHORIZED)
    fun Denied(status: AuthorizationStatus, message: String) =
      AuthorizationResult(false, status, message)
  }
}

/**
 * Enforces authorization policies for sensitive Space management operations.
 */
class SpaceAuthorizationManager(
  private val isSpaceUnlockedProvider: (String) -> Boolean,
  private val isLauncherLockedProvider: () -> Boolean
) {

  /**
   * Authorizes or rejects a [SensitiveOperation] on a [Space] with full context.
   */
  fun authorize(
    space: Space,
    operation: SensitiveOperation,
    context: AuthorizationContext = AuthorizationContext.Default
  ): AuthorizationResult {
    // 1. Unknown or corrupted policy fail-closed check
    val method = AuthenticationMethod.fromAuthPolicy(space.authPolicy)
    if (method == AuthenticationMethod.UNKNOWN) {
      return AuthorizationResult.Denied(
        AuthorizationStatus.DENIED_UNKNOWN_POLICY,
        "Space '${space.name}' has an unrecognized or corrupt security policy."
      )
    }

    // 2. If launcher itself is locked, no sensitive operations are permitted except UNLOCK_SPACE
    if (isLauncherLockedProvider() && operation != SensitiveOperation.UNLOCK_SPACE) {
      return AuthorizationResult.Denied(
        AuthorizationStatus.DENIED_LAUNCHER_LOCKED,
        "Launcher is locked. Unlock launcher before accessing space settings."
      )
    }

    // 3. Unprotected spaces
    if (!space.isProtected) {
      if (operation == SensitiveOperation.DISABLE_AUTHENTICATION) {
        return AuthorizationResult.Denied(
          AuthorizationStatus.DENIED_NOT_CONFIGURED,
          "Cannot disable protection on an unprotected Space."
        )
      }
      return AuthorizationResult.Authorized
    }

    // 4. Corrupted protected space verification
    if (!space.isSecurityConfigured) {
      return AuthorizationResult.Denied(
        AuthorizationStatus.DENIED_NOT_CONFIGURED,
        "Space '${space.name}' security configuration is corrupted or incomplete."
      )
    }

    // 5. UNLOCK_SPACE is always permitted to be attempted
    if (operation == SensitiveOperation.UNLOCK_SPACE) {
      return AuthorizationResult.Authorized
    }

    // 6. High-risk operations require explicit fresh authentication
    if (requiresExplicitAuthentication(space, operation)) {
      if (!context.hasExplicitAuthentication) {
        return AuthorizationResult.Denied(
          AuthorizationStatus.DENIED_REQUIRES_EXPLICIT_AUTH,
          "Fresh explicit authentication is required to perform ${operation.name} on '${space.name}'."
        )
      }
      return AuthorizationResult.Authorized
    }

    // 7. Standard protected operations require an active unlocked session
    if (!isSpaceUnlockedProvider(space.id)) {
      return AuthorizationResult.Denied(
        AuthorizationStatus.DENIED_SESSION_LOCKED,
        "Space '${space.name}' is currently locked. Authenticate to proceed."
      )
    }

    return AuthorizationResult.Authorized
  }

  /**
   * Quick boolean check determining whether the operation is authorized with default context.
   */
  fun canPerform(space: Space, operation: SensitiveOperation): Boolean {
    // If explicit authentication is required, default context cannot perform it
    if (space.isProtected && requiresExplicitAuthentication(space, operation)) {
      return false
    }
    return authorize(space, operation, AuthorizationContext.Default).isAuthorized
  }

  /**
   * Checks whether the space requires explicit fresh re-authentication for this operation.
   */
  fun requiresExplicitAuthentication(space: Space, operation: SensitiveOperation): Boolean {
    if (!space.isProtected) return false
    return when (operation) {
      SensitiveOperation.DELETE_SPACE,
      SensitiveOperation.CHANGE_AUTHENTICATION,
      SensitiveOperation.DISABLE_AUTHENTICATION,
      SensitiveOperation.CHANGE_SECURITY_SETTINGS -> true
      SensitiveOperation.EDIT_SPACE,
      SensitiveOperation.MANAGE_MEMBERSHIPS -> !isSpaceUnlockedProvider(space.id)
      SensitiveOperation.UNLOCK_SPACE -> !isSpaceUnlockedProvider(space.id)
    }
  }
}
