# Repository Snapshot — Lifecycle, Package Changes, and Recovery Hardening

## Snapshot Date
2026-08-29

## Current Git Commit / Branch
* **Commit:** UNKNOWN (Lifecycle, Package Changes, and Recovery Hardening implementation)
* **Branch:** main / workspace

---

## Build Status
* **Command:** `gradle assembleDebug` / `compile_applet`
* **Result:** `BUILD SUCCESS`
* **Artifact:** `app/build/outputs/apk/debug/app-debug.apk`

---

## Project Structure Tree
```text
.
├── .env.example
├── .gitignore
├── app/
│   ├── .gitignore
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/
│           │       └── example/
│           │           ├── MainActivity.kt
│           │           ├── diagnostics/
│           │           │   └── AppLogger.kt
│           │           ├── domain/
│           │           │   ├── DomainContract.kt
│           │           │   ├── model/
│           │           │   │   ├── DiscoveredApp.kt
│           │           │   │   ├── Space.kt
│           │           │   │   └── SpaceMembership.kt
│           │           │   └── repository/
│           │           │       └── SpaceRepository.kt
│           │           ├── data/
│           │           │   ├── DataContract.kt
│           │           │   ├── database/
│           │           │   │   └── LauncherDatabase.kt
│           │           │   ├── entity/
│           │           │   │   ├── SpaceEntity.kt
│           │           │   │   └── SpaceMembershipEntity.kt
│           │           │   ├── dao/
│           │           │   │   ├── SpaceDao.kt
│           │           │   │   └── SpaceMembershipDao.kt
│           │           │   ├── preferences/
│           │           │   │   └── LauncherPreferences.kt
│           │           │   └── repository/
│           │           │       └── RoomSpaceRepository.kt
│           │           ├── platform/
│           │           │   ├── PlatformContract.kt
│           │           │   ├── HomePlatformManager.kt
│           │           │   ├── AppDiscoveryManager.kt
│           │           │   ├── AppLaunchManager.kt
│           │           │   └── PinSecurityManager.kt
│           │           ├── presentation/
│           │           │   ├── AppDiscoveryViewModel.kt
│           │           │   ├── SpaceViewModel.kt
│           │           │   ├── LauncherHomeScreen.kt
│           │           │   ├── LauncherConfigurationScreen.kt
│           │           │   ├── AppCatalogScreen.kt
│           │           │   ├── SpacePinDialogs.kt
│           │           │   ├── SpaceCustomizationDialog.kt
│           │           │   ├── SpaceManagementComponents.kt
│           │           │   ├── FoundationOverviewScreen.kt
│           │           │   └── LauncherDiagnosticsScreen.kt
│           │           └── ui/
│           │               └── theme/
│           │                   ├── Color.kt
│           │                   ├── Theme.kt
│           │                   └── Type.kt
│           └── res/
│               ├── drawable/
│               ├── mipmap-*/
│               ├── values/
│               │   ├── colors.xml
│               │   ├── strings.xml
│               │   └── themes.xml
│               └── xml/
├── docs/
│   ├── PROJECT_CONSTITUTION.md
│   ├── PROJECT_STATE.md
│   ├── ARCHITECTURE.md
│   ├── DECISIONS.md
│   ├── PHYSICAL_TEST_LOG.md
│   ├── TEST_DEVICE.md
│   ├── CURRENT_TASK.md
│   ├── AGENT_HANDOFF.md
│   └── REPOSITORY_SNAPSHOT.md
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
├── build.gradle.kts
├── gradle.properties
├── metadata.json
└── settings.gradle.kts
```

---

## Key Dependency Coordinates
* **Kotlin:** `2.2.10`
* **Jetpack Compose BOM:** `2024.09.00`
* **Room Database & KSP:** `2.6.1`
* **Jetpack DataStore:** `1.1.1`
* **Coil Compose:** `2.6.0`
* **AndroidX Lifecycle / ViewModel:** `2.8.5`
* **Compile SDK / Target SDK:** `36`
* **Min SDK:** `28` (Android 9.0+)
