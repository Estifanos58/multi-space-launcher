# Contributing to Multi-Space Launcher

Thank you for your interest in contributing to Multi-Space Launcher!

## Code Style & Architecture Guidelines

1. **Language & Tooling**:
   - Write code in 100% Kotlin using Jetpack Compose for UI.
   - Use Android Studio's standard Kotlin style guide.
2. **Layering**:
   - Keep domain models pure (no Android framework imports in `domain`).
   - Platform-specific code (`LauncherApps`, `RoleManager`, etc.) must reside under `platform`.
   - Data persistence logic must reside in `data` (Room DAOs, DataStore).
   - UI Composables and ViewModels must reside in `presentation`.
3. **Branching & Pull Requests**:
   - Fork the repository and create a feature branch (`feature/my-new-feature`).
   - Ensure the project builds cleanly before opening a PR:
     ```bash
     ./gradlew assembleDebug
     ```
   - Write descriptive commit messages explaining the rationale behind your changes.
