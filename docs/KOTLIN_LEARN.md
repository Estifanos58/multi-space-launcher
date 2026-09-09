# Kotlin Masterclass: Learning Modern Android Through the Multi-Space Launcher Codebase

Welcome to the **Multi-Space Launcher Kotlin Masterclass**. This guide is designed to teach you Kotlin not through toy examples or synthetic tutorials, but through the architecture, algorithms, and idiomatic patterns of a production-grade, multi-tenant Android Home Launcher application: **Multi-Space Launcher**.

Whether you are transitioning from Java, Swift, TypeScript, or C++, or leveling up your modern Android development skills, this document breaks down the language features, architectural principles, reactive streams, and custom subsystems that make this application fast, resilient, and elegant.

---

# Table of Contents

- [PART 1: Kotlin Language Deep Dive (Through Real Project Code)](#part-1-kotlin-language-deep-dive-through-real-project-code)
  - [1. Fundamental Syntax & Language Features](#1-fundamental-syntax--language-features)
    - [1.1 Variables, Mutability, and Type Inference](#11-variables-mutability-and-type-inference)
    - [1.2 String Templates and Multi-Line Text](#12-string-templates-and-multi-line-text)
    - [1.3 Control Flow as Expressions (`if`, `when`, and ranges)](#13-control-flow-as-expressions-if-when-and-ranges)
    - [1.4 Null Safety: Safe Calls, Elvis Operator, and Smart Casting](#14-null-safety-safe-calls-elvis-operator-and-smart-casting)
  - [2. The Object-Oriented & Type System](#2-the-object-oriented--type-system)
    - [2.1 Data Classes, Immutability, and `copy()`](#21-data-classes-immutability-and-copy)
    - [2.2 Interfaces, Polymorphism, and Inversion of Control](#22-interfaces-polymorphism-and-inversion-of-control)
    - [2.3 Abstract Classes and Inheritance](#23-abstract-classes-and-inheritance)
    - [2.4 Object Declarations, Singletons, and Anonymous Objects](#24-object-declarations-singletons-and-anonymous-objects)
    - [2.5 Companion Objects and Static Factories](#25-companion-objects-and-static-factories)
    - [2.6 Enums vs. Sealed Classes](#26-enums-vs-sealed-classes)
    - [2.7 Generics and Variance](#27-generics-and-variance)
  - [3. Functional Programming & Idiomatic Kotlin](#3-functional-programming--idiomatic-kotlin)
    - [3.1 Higher-Order Functions, Lambdas, and Trailing Lambda Syntax](#31-higher-order-functions-lambdas-and-trailing-lambda-syntax)
    - [3.2 Extension Functions and Extension Properties](#32-extension-functions-and-extension-properties)
    - [3.3 Scope Functions: `let`, `apply`, `also`, `run`, `with`](#33-scope-functions-let-apply-also-run-with)
    - [3.4 Collections, Transformations, and Sequences](#34-collections-transformations-and-sequences)
    - [3.5 Property Delegation (`by lazy`, `by viewModels()`, `by remember`)](#35-property-delegation-by-lazy-by-viewmodels-by-remember)
  - [4. Asynchronous Concurrency: Coroutines & Reactive Flow](#4-asynchronous-concurrency-coroutines--reactive-flow)
    - [4.1 Suspend Functions and Structured Concurrency](#41-suspend-functions-and-structured-concurrency)
    - [4.2 Dispatchers and Coroutine Scopes (`viewModelScope`)](#42-dispatchers-and-coroutine-scopes-viewmodelscope)
    - [4.3 Reactive Streams: `Flow`, `StateFlow`, and `SharedFlow`](#43-reactive-streams-flow-stateflow-and-sharedflow)
    - [4.4 Advanced Flow Operators: `flatMapLatest`, `combine`, `stateIn`](#44-advanced-flow-operators-flatmaplatest-combine-statein)
  - [5. Error Handling, Robustness, and Contracts](#5-error-handling-robustness-and-contracts)
    - [5.1 Preconditions: `require()`, `check()`, and Invariants](#51-preconditions-require-check-and-invariants)
    - [5.2 Functional Error Handling: `Result<T>` and `runCatching`](#52-functional-error-handling-resultt-and-runcatching)

---

- [PART 2: Architecture & Implementation of Multi-Space Launcher](#part-2-architecture--implementation-of-multi-space-launcher)
  - [6. Project Architecture & Layer Relationships](#6-project-architecture--layer-relationships)
    - [6.1 Package Directory Breakdown](#61-package-directory-breakdown)
    - [6.2 Unidirectional Dependency Flow](#62-unidirectional-dependency-flow)
  - [7. Application Startup & Android Lifecycle Management](#7-application-startup--android-lifecycle-management)
    - [7.1 `MainActivity` as the System Launcher Activity](#71-mainactivity-as-the-system-launcher-activity)
    - [7.2 RoleManager and Default Home Detection](#72-rolemanager-and-default-home-detection)
    - [7.3 Lifecycle Interceptions: `ACTION_SCREEN_OFF` and `onNewIntent`](#73-lifecycle-interceptions-action_screen_off-and-onnewintent)
  - [8. Navigation & The Two-Layer Spatial Model](#8-navigation--the-two-layer-spatial-model)
    - [8.1 Layer 1 (Curated Desk) vs. Layer 2 (App Library)](#81-layer-1-curated-desk-vs-layer-2-app-library)
    - [8.2 Security Interception & Lock Screen Transition](#82-security-interception--lock-screen-transition)
  - [9. Jetpack Compose UI Engine & State Management](#9-jetpack-compose-ui-engine--state-management)
    - [9.1 Declarative State Hoisting and Recomposition Control](#91-declarative-state-hoisting-and-recomposition-control)
    - [9.2 Interactive Canvas Rendering (`PatternLockCanvas`)](#92-interactive-canvas-rendering-patternlockcanvas)
    - [9.3 Touch Gestures & Pointer Input Subsystem](#93-touch-gestures--pointer-input-subsystem)
  - [10. Data Persistence: Room Database & DataStore](#10-data-persistence-room-database--datastore)
    - [10.1 Room Database, Schema Versions & SQLite Migrations](#101-room-database-schema-versions--sqlite-migrations)
    - [10.2 DAOs, Transactions & Live SQLite Query Streams](#102-daos-transactions--live-sqlite-query-streams)
    - [10.3 Entity to Domain Model Mapping](#103-entity-to-domain-model-mapping)
    - [10.4 Reactive Preferences with Jetpack DataStore](#104-reactive-preferences-with-jetpack-datastore)
  - [11. Subsystems & Algorithmic Highlights](#11-subsystems--algorithmic-highlights)
    - [11.1 Spatial Footprint & Ripple Displacement (`PlacementCascadeHelper`)](#111-spatial-footprint--ripple-displacement-placementcascadehelper)
    - [11.2 3D Page Turn Transitions (`PageTurnTransformer`)](#112-3d-page-turn-transitions-pageturntransformer)
    - [11.3 Desktop Widgets & Dynamic Resizing Subsystem](#113-desktop-widgets--dynamic-resizing-subsystem)
    - [11.4 Security Subsystem: PBKDF2 Hashing & Salted Validation](#114-security-subsystem-pbkdf2-hashing--salted-validation)
  - [12. Testing Architecture & Verification](#12-testing-architecture--verification)
    - [12.1 Pure JVM Unit Testing for Critical Business Logic](#121-pure-jvm-unit-testing-for-critical-business-logic)
    - [12.2 Test Structure & Assertions in Real Tests](#122-test-structure--assertions-in-real-tests)

---

# PART 1: Kotlin Language Deep Dive (Through Real Project Code)

---

## 1. Fundamental Syntax & Language Features

### 1.1 Variables, Mutability, and Type Inference

Kotlin enforces immutability at compile time using two keywords:
- `val`: **Value** — Read-only reference (immutable binding). Once assigned, it cannot be reassigned.
- `var`: **Variable** — Mutable reference.

In the Multi-Space Launcher codebase, `val` is used for over 95% of declarations to prevent unintentional state mutations and concurrency race conditions.

```kotlin
// From app/src/main/java/com/multispace/platform/PinSecurityManager.kt
private const val ITERATION_COUNT = 10_000
private const val KEY_LENGTH_BITS = 256
private const val SALT_LENGTH_BYTES = 16
```
> **Idiom Note:** `const val` indicates a compile-time constant (inlined by the compiler), permissible only at top-level or inside `object`/`companion object` declarations for primitive types and `String`.

Kotlin provides **type inference**, meaning the compiler automatically deduces types without requiring explicit type signatures:

```kotlin
// Explicit: val random: SecureRandom = SecureRandom()
// Inferred (idiomatic):
val random = SecureRandom()
val salt = ByteArray(SALT_LENGTH_BYTES)
```

However, public API return types and abstract properties frequently retain explicit signatures for interface contracts:
```kotlin
// From app/src/main/java/com/multispace/data/repository/RoomSpaceRepository.kt
override val allSpacesFlow: Flow<List<Space>> = spaceDao.getAllSpacesFlow().map { entities ->
  entities.map { it.toDomain() }
}
```

---

### 1.2 String Templates and Multi-Line Text

Instead of string concatenation (`"Hello " + name + "!"`) or `String.format()`, Kotlin provides **string templates** evaluated with `$variable` or `${expression}`:

```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
val action = intent?.action ?: "null"
val categories = intent?.categories?.joinToString(",") ?: "none"
val flags = intent?.flags?.let { "0x" + Integer.toHexString(it) } ?: "0x0"

AppLogger.i(
  AppLogger.Category.LIFECYCLE,
  "MainActivity $event -> taskId=$taskId, isTaskRoot=$isTaskRoot, action=$action, categories=[$categories], flags=$flags"
)
```

Notice how `$taskId` and `$isTaskRoot` are directly interpolated, while `[$categories]` wraps the evaluated variable in literal brackets.

For complex SQL migration scripts, Kotlin supports triple-quoted raw strings (`"""`), which preserve indentation, line breaks, and prevent escape character confusion:
```kotlin
// From app/src/main/java/com/multispace/data/database/LauncherDatabase.kt
db.execSQL("""
  CREATE TABLE IF NOT EXISTS space_dock_items (
    id TEXT PRIMARY KEY NOT NULL,
    space_id TEXT NOT NULL,
    position_index INTEGER NOT NULL,
    package_name TEXT NOT NULL,
    activity_name TEXT NOT NULL,
    user_id INTEGER NOT NULL DEFAULT 0,
    label TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
  )
""".trimIndent())
```
`.trimIndent()` detects the minimal common indentation margin and cleanly removes it at runtime.

---

### 1.3 Control Flow as Expressions (`if`, `when`, and ranges)

In Kotlin, `if` and `when` are **expressions**, meaning they evaluate to a value and can be assigned directly to a variable or returned from a function.

#### `if` as an Expression:
```kotlin
// From app/src/main/java/com/multispace/presentation/SpaceViewModel.kt
fun toggleLayer() {
  _activeLayerIndex.value = if (_activeLayerIndex.value == 1) 2 else 1
}
```
No ternary operator (`? :`) exists in Kotlin because `if (...) a else b` acts as the ternary expression with complete type safety.

#### `when` as a Powerful Expression:
`when` replaces Java/C++ `switch` statements and can match values, types, ranges, or arbitrary Boolean conditions:

```kotlin
// From app/src/main/java/com/multispace/presentation/PageTurnTransformer.kt
return when (effect) {
  PageTurnEffect.NORMAL -> PageTransformation()

  PageTurnEffect.CUBE -> {
    val rotationY = if (clampedOffset > 0) 90f * clampedOffset else 90f * clampedOffset
    val pivotX = if (clampedOffset > 0) 1.0f else 0.0f
    PageTransformation(
      translationX = 0f,
      rotationY = rotationY,
      transformOriginX = pivotX,
      alpha = 1f - (absOffset * 0.3f * safeIntensity)
    )
  }

  PageTurnEffect.CROSSFADE -> {
    PageTransformation(
      translationX = -clampedOffset * pageWidth,
      alpha = (1f - absOffset * safeIntensity).coerceIn(0f, 1f)
    )
  }

  PageTurnEffect.WINDMILL -> { /* ... */ }
  PageTurnEffect.ZOOM -> { /* ... */ }
}
```

> **Exhaustiveness Guarantee:** When `when` is used as an expression on an `enum` or `sealed class`, Kotlin forces you to cover every possible branch. If a new `PageTurnEffect` is added to the enum later, the compiler immediately halts the build until all `when` expressions handle it.

#### Ranges and Progression Operators:
Kotlin provides first-class syntax for numerical ranges:
- `0 until count`: Half-open range `[0, count)` (does not include `count`).
- `0..count`: Closed range `[0, count]` (includes `count`).
- `downTo`, `step`, and `in`:

```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt
for (dr in 0 until spanY) {
  for (dc in 0 until spanX) {
    val r = row + dr
    val c = col + dc
    slots.add(pageIndex * pageSize + (r * cols + c))
  }
}
```

---

### 1.4 Null Safety: Safe Calls, Elvis Operator, and Smart Casting

The Kotlin type system distinguishes between nullable types (`String?`) and non-nullable types (`String`). Null pointer exceptions (`NullPointerException`) are eliminated at compile time unless explicitly bypassed.

#### The Safe Call Operator (`?.`) and Elvis Operator (`?:`)
```kotlin
// From app/src/main/java/com/multispace/domain/model/Space.kt
val isProtected: Boolean
  get() = if (authPolicy == AUTH_BIOMETRIC) true
          else ((authPolicy == AUTH_PIN || authPolicy == AUTH_PATTERN) &&
                !pinHash.isNullOrEmpty() && !pinSalt.isNullOrEmpty())
```

In `MainActivity.kt`:
```kotlin
// Safely navigate nested nullable references, providing a fallback if null:
val action = intent?.action ?: "null"
val categories = intent?.categories?.joinToString(",") ?: "none"
val flags = intent?.flags?.let { "0x" + Integer.toHexString(it) } ?: "0x0"
```
- `intent?.action`: If `intent` is null, returns `null` instead of crashing.
- `?: "null"`: The Elvis operator evaluates the right side **only** if the left side evaluates to `null`.

#### Smart Casting
Once the compiler verifies that a variable is not null (or checks its type with `is`), it automatically casts the variable inside that scope:

```kotlin
// From app/src/main/java/com/multispace/presentation/SpaceViewModel.kt
val activeMemberships: StateFlow<List<SpaceMembership>> = spaceRepository.activeSpaceFlow
  .flatMapLatest { space ->
    if (space != null) {
      // Inside this block, 'space' is smart-cast from Space? to non-null Space!
      spaceRepository.getMembershipsForSpaceFlow(space.id)
    } else {
      flowOf(emptyList())
    }
  }
```
You never need to write manual casts like `((Space) space).getId()`.

---

## 2. The Object-Oriented & Type System

### 2.1 Data Classes, Immutability, and `copy()`

In domain-driven design, data holders should be immutable, self-describing, and easy to clone with modifications. In Kotlin, prefixing a class with `data` generates:
1. `equals()` and `hashCode()` based on all constructor properties.
2. `toString()` printing readable property values (e.g., `Space(id=s1, name=Work)`).
3. `componentN()` functions for destructuring declarations (`val (id, name) = space`).
4. `copy()` for non-destructive mutation.

```kotlin
// From app/src/main/java/com/multispace/domain/model/SpaceItemPlacement.kt
data class SpaceItemPlacement(
  val id: String,
  val spaceId: String,
  val pageIndex: Int,
  val positionIndex: Int,
  val packageName: String,
  val activityName: String = "",
  val userId: Int = 0,
  val label: String = "",
  val spanX: Int = 1,
  val spanY: Int = 1,
  val itemType: String = TYPE_APP,
  val appWidgetId: Int? = null,
  val customWidgetType: String? = null,
  val folderId: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
```

#### Non-Destructive Mutation with `copy()`:
To move an item to a new slot or page, we never mutate properties in place. Instead, we produce a new instance using `copy()`:
```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt
val movedItem = item.copy(
  pageIndex = targetPage,
  positionIndex = newPosition,
  updatedAt = System.currentTimeMillis()
)
```
All other 13 properties of `SpaceItemPlacement` remain unchanged, while `pageIndex`, `positionIndex`, and `updatedAt` are updated cleanly.

---

### 2.2 Interfaces, Polymorphism, and Inversion of Control

Interfaces in Kotlin can declare abstract properties as well as methods, and may even contain default implementations:

```kotlin
// From app/src/main/java/com/multispace/domain/repository/SpaceRepository.kt
interface SpaceRepository {
  val allSpacesFlow: Flow<List<Space>>
  val activeSpaceFlow: Flow<Space?>
  val activeSpaceIdFlow: Flow<String?>

  suspend fun ensureDefaultSpaceInitialized(initialApps: List<DiscoveredApp> = emptyList()): Result<Space>
  suspend fun createSpace(name: String): Result<Space>
  suspend fun updateSpace(space: Space): Result<Unit>
  suspend fun deleteSpace(spaceId: String): Result<Unit>
  // ...
}
```

The concrete implementation lives in the `data` layer (`RoomSpaceRepository`), decoupling the ViewModel and UI from Room or SQLite specifics:

```kotlin
class RoomSpaceRepository(
  private val spaceDao: SpaceDao,
  private val membershipDao: SpaceMembershipDao,
  private val layoutDao: SpaceLayoutDao,
  private val preferences: LauncherPreferences
) : SpaceRepository {
  // Implementation details...
}
```

---

### 2.3 Abstract Classes and Inheritance

Inheritance is opt-in in Kotlin. All classes and methods are `final` by default. To allow subclassing, a class must be marked `open` or `abstract`:

```kotlin
// From app/src/main/java/com/multispace/data/database/LauncherDatabase.kt
@Database(
  entities = [
    SpaceEntity::class,
    SpaceMembershipEntity::class,
    SpaceItemPlacementEntity::class,
    SpaceFolderEntity::class,
    SpaceFolderItemEntity::class,
    SpaceDockItemEntity::class
  ],
  version = 8,
  exportSchema = false
)
abstract class LauncherDatabase : RoomDatabase() {
  abstract fun spaceDao(): SpaceDao
  abstract fun spaceMembershipDao(): SpaceMembershipDao
  abstract fun spaceLayoutDao(): SpaceLayoutDao
  // ...
}
```
Room's annotation processor generates the concrete subclass at compile time.

---

### 2.4 Object Declarations, Singletons, and Anonymous Objects

Kotlin has no static methods or fields. Instead, it provides `object` declarations, creating thread-safe, lazily initialized singletons natively:

#### 1. Pure Singleton Utilities
```kotlin
// From app/src/main/java/com/multispace/platform/PinSecurityManager.kt
object PinSecurityManager {
  fun generateSalt(): String { /* ... */ }
  fun hashPin(pin: String, saltBase64: String): String { /* ... */ }
  fun verifyPin(enteredPin: String, saltBase64: String?, expectedHashBase64: String?): Boolean { /* ... */ }
}
```
Call sites directly invoke methods without instantiation:
```kotlin
val salt = PinSecurityManager.generateSalt()
val hash = PinSecurityManager.hashPin("1234", salt)
```

#### 2. Domain Calculation Singletons
```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt
object PlacementCascadeHelper {
  fun computeFullPlacementsAfterDrop(/*...*/): List<SpaceItemPlacement> { /*...*/ }
}
```

#### 3. Anonymous Object Expressions (Replacing Java Anonymous Classes)
```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
private val screenOffReceiver = object : android.content.BroadcastReceiver() {
  override fun onReceive(context: android.content.Context?, intent: Intent?) {
    if (intent?.action == Intent.ACTION_SCREEN_OFF) {
      AppLogger.i(AppLogger.Category.LIFECYCLE, "Screen turned off -> Locking Multi-Space")
      spaceViewModel.lockPhone()
    }
  }
}
```

---

### 2.5 Companion Objects and Static Factories

When a class needs factory methods or associated constants, they are placed inside a `companion object`. The companion object belongs to the class itself:

```kotlin
// From app/src/main/java/com/multispace/domain/model/Space.kt
class Space(/*...*/) {
  companion object {
    const val DEFAULT_SPACE_ID = "space_default"
    const val DEFAULT_SPACE_NAME = "Default"
    const val AUTH_NONE = "NONE"
    const val AUTH_PIN = "PIN"
    const val AUTH_PATTERN = "PATTERN"
    const val AUTH_BIOMETRIC = "BIOMETRIC"
  }
}

// In LauncherDatabase.kt: Singleton Thread-Safe Double-Checked Locking Pattern
companion object {
  @Volatile
  private var INSTANCE: LauncherDatabase? = null

  fun getInstance(context: Context): LauncherDatabase {
    return INSTANCE ?: synchronized(this) {
      val instance = Room.databaseBuilder(
        context.applicationContext,
        LauncherDatabase::class.java,
        "multispace_launcher.db"
      )
      .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
      .build()
      INSTANCE = instance
      instance
    }
  }
}
```

---

### 2.6 Enums vs. Sealed Classes

#### Enums: Fixed Set of Static Values
Enums represent a closed set of constant choices with optional parameters:
```kotlin
// From app/src/main/java/com/multispace/domain/model/PageTurnEffect.kt
enum class PageTurnEffect(
  val displayName: String,
  val description: String,
  val supportsIntensity: Boolean = true
) {
  NORMAL("Classic Slide", "Smooth horizontal translation", false),
  CUBE("3D Cube", "Rotates desktop pages in a 3D isometric cube", true),
  CROSSFADE("Smooth Crossfade", "Fades between pages smoothly", false),
  WINDMILL("Windmill Fan", "Pivots pages around lower center corner", true),
  ZOOM("Zoom Depth", "Scales outgoing page down into background", true);

  companion object {
    fun fromName(name: String?): PageTurnEffect {
      return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NORMAL
    }
  }
}
```
> **Modern Kotlin note:** Notice `entries` instead of `values()`. In Kotlin 1.9+, `entries` provides an immutable pre-allocated list without allocating a new array on every call.

#### Sealed Classes: Algebraic Data Types with Dynamic State
When each state can hold different data fields (e.g., UI States or Operation Results), use `sealed interface` or `sealed class`:
```kotlin
sealed interface LauncherUiState {
  data object Loading : LauncherUiState
  data class Success(val spaces: List<Space>, val active: Space) : LauncherUiState
  data class Error(val cause: Throwable, val userMessage: String) : LauncherUiState
}
```

---

### 2.7 Generics and Variance

Kotlin uses declaration-site variance (`out` for producers/covariance, `in` for consumers/contravariance):

```kotlin
// Kotlin's List<out E> is covariant:
val apps: List<DiscoveredApp> = emptyList()
val items: List<Any> = apps // Allowed because List is defined as List<out E>
```

In Room DAOs and Repositories, generics provide type-safe database queries and transforms:
```kotlin
// From app/src/main/java/com/multispace/data/dao/SpaceDao.kt
@Dao
interface SpaceDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpace(space: SpaceEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpaces(spaces: List<SpaceEntity>): List<Long>
}
```

---

## 3. Functional Programming & Idiomatic Kotlin

### 3.1 Higher-Order Functions, Lambdas, and Trailing Lambda Syntax

A **higher-order function** is a function that takes a function as a parameter or returns a function.

```kotlin
// A function parameter with signature: (DiscoveredApp) -> Unit
@Composable
fun Layer1HomeScreen(
  onLaunchApp: (DiscoveredApp) -> Unit,
  onOpenCustomization: () -> Unit
)
```

#### Trailing Lambda Syntax:
If the last parameter of a function is a lambda, Kotlin allows you to place the lambda expression **outside the parentheses**:

```kotlin
// Instead of:
// Button({ doSomething() })
// Idiomatic Kotlin:
Button(
  onClick = {
    spaceViewModel.setLayer(1)
  },
  modifier = Modifier.testTag("back_to_home_button")
) {
  Text("Return to Desk")
}
```

---

### 3.2 Extension Functions and Extension Properties

Extension functions allow you to "add" functions to a class without inheriting from it or modifying its source code. They are resolved statically.

#### 1. Domain Transformation Extensions
In Clean Architecture, data entities (`SpaceEntity`) must be translated into domain models (`Space`):
```kotlin
// From app/src/main/java/com/multispace/data/entity/SpaceEntity.kt
fun SpaceEntity.toDomain(): Space {
  return Space(
    id = this.id,
    name = this.name,
    orderIndex = this.orderIndex,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    authPolicy = this.authPolicy,
    pinSalt = this.pinSalt,
    pinHash = this.pinHash,
    layoutType = this.layoutType,
    pageTurnEffect = PageTurnEffect.fromName(this.pageTurnEffect)
  )
}

fun Space.toEntity(): SpaceEntity {
  return SpaceEntity(
    id = this.id,
    name = this.name,
    orderIndex = this.orderIndex,
    createdAt = this.createdAt,
    updatedAt = this.updatedAt,
    authPolicy = this.authPolicy,
    pinSalt = this.pinSalt,
    pinHash = this.pinHash,
    layoutType = this.layoutType,
    pageTurnEffect = this.pageTurnEffect.name
  )
}
```
Now any `SpaceEntity` instance has a `.toDomain()` method, and any `Space` instance has a `.toEntity()` method!

#### 2. Jetpack Compose UI Extensions
In `PageTurnTransformer.kt`, an extension on Compose's `Modifier` encapsulates 3D GPU transformations:
```kotlin
// From app/src/main/java/com/multispace/presentation/PageTurnTransformer.kt
fun Modifier.pageTurnEffect(
  pagerState: PagerState,
  page: Int,
  effect: PageTurnEffect,
  pageWidth: Float = 1080f,
  intensity: Float = 1.0f
): Modifier = this.graphicsLayer {
  val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
  val transformation = calculatePageTransformation(
    effect = effect,
    pageOffset = pageOffset,
    pageWidth = pageWidth,
    intensity = intensity
  )

  translationX = transformation.translationX
  scaleX = transformation.scaleX
  scaleY = transformation.scaleY
  rotationY = transformation.rotationY
  rotationZ = transformation.rotationZ
  transformOrigin = TransformOrigin(transformation.transformOriginX, transformation.transformOriginY)
  cameraDistance = transformation.cameraDistanceMultiplier
  alpha = transformation.alpha
}
```

---

### 3.3 Scope Functions: `let`, `apply`, `also`, `run`, `with`

Scope functions execute a block of code within the context of an object. The differences lie in **how the object is referenced (`it` vs. `this`)** and **what the expression returns (the result of the lambda vs. the context object)**:

| Function | Context Reference | Return Value | Primary Use Case |
|---|---|---|---|
| `let` | `it` | Lambda result | Null-checks (`?.let`) and transforming values |
| `apply` | `this` | Context object | Configuring object properties (builder pattern) |
| `also` | `it` | Context object | Side-effects (logging, caching, debugging) |
| `run` | `this` | Lambda result | Object configuration + computing a result |
| `with` | `this` | Lambda result | Grouping calls on an object without re-typing its name |

#### Real Examples from Multi-Space Launcher:

**1. `apply` for Intent Configuration:**
```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
val intent = Intent(this, ConfigurationActivity::class.java).apply {
  flags = Intent.FLAG_ACTIVITY_NEW_TASK
  putExtra("EXTRA_SPACE_ID", activeSpaceId)
}
startActivity(intent)
```

**2. `let` for Safe Null Transforms:**
```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
val flags = intent?.flags?.let { "0x" + Integer.toHexString(it) } ?: "0x0"
```

**3. `also` for Side-Effect Logging:**
```kotlin
fun selectSpace(space: Space): Space {
  return space.also {
    AppLogger.i(AppLogger.Category.LAUNCHER, "Switched active space to: ${it.name}")
  }
}
```

---

### 3.4 Collections, Transformations, and Sequences

Multi-Space Launcher makes heavy use of Kotlin's functional collection APIs for calculating grid layouts, filtering apps, and reordering dock items:

```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt

// 1. filter and associateBy
val nonColliding = existingPlacements
  .filter { it.id != itemToInsert.id }
  .associateBy { it.positionIndex }

// 2. map and sortedBy
val sortedPlacements = placements
  .sortedBy { it.positionIndex }
  .mapIndexed { index, placement ->
    placement.copy(positionIndex = index)
  }

// 3. groupBy
val placementsByPage: Map<Int, List<SpaceItemPlacement>> = placements.groupBy { it.pageIndex }

// 4. any, all, none
val hasCollision = incomingFootprint.globalSlots.any { slot -> occupiedSlots.contains(slot) }

// 5. maxOfOrNull, minOfOrNull, coerceIn
val maxPage = placements.maxOfOrNull { it.pageIndex } ?: 0
val clampedZoom = zoomLevel.coerceIn(0.5f, 3.0f)
```

Kotlin distinguishes between read-only interfaces (`List<T>`, `Set<T>`, `Map<K, V>`) and mutable ones (`MutableList<T>`, `MutableSet<T>`). We always expose read-only collections across public API boundaries:
```kotlin
private val _installedApps = mutableStateListOf<DiscoveredApp>()
val installedApps: List<DiscoveredApp> get() = _installedApps
```

---

### 3.5 Property Delegation (`by lazy`, `by viewModels()`, `by remember`)

Kotlin delegation delegates property getter/setter logic to an auxiliary object via the `by` keyword.

#### 1. `by lazy`: Thread-Safe Lazy Initialization
Computed once on first access, then cached:
```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt
val globalSlots: Set<Int> by lazy {
  val slots = mutableSetOf<Int>()
  for (dr in 0 until spanY) {
    for (dc in 0 until spanX) {
      slots.add(pageIndex * pageSize + ((row + dr) * cols + (col + dc)))
    }
  }
  slots // Returned and cached forever for this Footprint instance
}
```

#### 2. `by viewModels()`: Android Lifecycle ViewModel Delegation
```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
private val discoveryViewModel: AppDiscoveryViewModel by viewModels()
private val spaceViewModel: SpaceViewModel by viewModels()
```
The activity delegate automatically retrieves or creates the ViewModel instance bound to the Activity's `ViewModelStoreOwner`.

#### 3. `by remember` & `by mutableStateOf`: Jetpack Compose State Delegation
```kotlin
// Instead of accessing .value explicitly every time:
var isDragging by remember { mutableStateOf(false) }
var dragOffset by remember { mutableStateOf(Offset.Zero) }
```

---

## 4. Asynchronous Concurrency: Coroutines & Reactive Flow

### 4.1 Suspend Functions and Structured Concurrency

A `suspend` function can pause execution without blocking the underlying OS thread, resuming when the async work completes:

```kotlin
// From app/src/main/java/com/multispace/domain/repository/SpaceRepository.kt
suspend fun createSpace(name: String): Result<Space>
suspend fun deleteSpace(spaceId: String): Result<Unit>
suspend fun savePlacements(placements: List<SpaceItemPlacement>): Result<Unit>
```

Under the hood, the Kotlin compiler transforms `suspend` functions into state machines using Continuation-Passing Style (CPS).

---

### 4.2 Dispatchers and Coroutine Scopes (`viewModelScope`)

Coroutines must run inside a `CoroutineScope`. In Android, `viewModelScope` is tied directly to the ViewModel's lifecycle. When the ViewModel is cleared (e.g., when the user closes the screen), all ongoing coroutines launched in `viewModelScope` are automatically cancelled, preventing memory leaks and orphaned background tasks.

```kotlin
// From app/src/main/java/com/multispace/presentation/SpaceViewModel.kt
fun createSpace(name: String) {
  viewModelScope.launch {
    val result = spaceRepository.createSpace(name)
    result.fold(
      onSuccess = { created ->
        _userFeedback.tryEmit("Space '${created.name}' created successfully.")
      },
      onFailure = { error ->
        _userFeedback.tryEmit("Error: ${error.message ?: "Failed to create Space."}")
      }
    )
  }
}
```

---

### 4.3 Reactive Streams: `Flow`, `StateFlow`, and `SharedFlow`

Multi-Space Launcher models all app data as reactive asynchronous data streams:

1. **`Flow<T>`**: Cold stream. Executes only when collected (e.g., Room database queries).
2. **`StateFlow<T>`**: Hot stream representing state. Always holds the latest value, replays it to new collectors, and conflates updates.
3. **`SharedFlow<T>`**: Hot stream representing one-off events (e.g., toast messages, navigation events).

```kotlin
// From app/src/main/java/com/multispace/presentation/SpaceViewModel.kt

// One-off UI feedback events (SharedFlow)
private val _userFeedback = MutableSharedFlow<String>(extraBufferCapacity = 8)
val userFeedback: SharedFlow<String> = _userFeedback.asSharedFlow()

// Persistent UI state (StateFlow)
private val _activeLayerIndex = MutableStateFlow(1)
val activeLayerIndex: StateFlow<Int> = _activeLayerIndex.asStateFlow()
```

---

### 4.4 Advanced Flow Operators: `flatMapLatest`, `combine`, `stateIn`

In `SpaceViewModel.kt`, reactive flows from the repository are dynamically transformed:

```kotlin
// From app/src/main/java/com/multispace/presentation/SpaceViewModel.kt

// 1. stateIn: Converts cold Flow to hot StateFlow scoped to viewModelScope
val allSpaces: StateFlow<List<Space>> = spaceRepository.allSpacesFlow
  .stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList()
  )

// 2. flatMapLatest: Whenever activeSpace changes, cancel previous placement observation
//    and switch to observing placements for the new space ID
val activePlacements: StateFlow<List<SpaceItemPlacement>> = spaceRepository.activeSpaceFlow
  .flatMapLatest { space ->
    if (space != null) {
      spaceRepository.getPlacementsForSpaceLayerFlow(space.id, SpaceItemPlacement.LAYER_HOME)
    } else {
      flowOf(emptyList())
    }
  }
  .stateIn(
    scope = viewModelScope,
    started = SharingStarted.Eagerly,
    initialValue = emptyList()
  )

// 3. combine: Merges multiple Flows into a single cohesive stream
override val activeSpaceFlow: Flow<Space?> = combine(
  allSpacesFlow,
  activeSpaceIdFlow
) { spaces, activeId ->
  if (spaces.isEmpty()) {
    null
  } else {
    spaces.firstOrNull { it.id == activeId } ?: spaces.first()
  }
}
```

---

## 5. Error Handling, Robustness, and Contracts

### 5.1 Preconditions: `require()`, `check()`, and Invariants

Kotlin provides standard library precondition functions that throw standard runtime exceptions with descriptive messages:
- `require(boolean)`: Throws `IllegalArgumentException` (for validating arguments).
- `check(boolean)`: Throws `IllegalStateException` (for validating internal state invariants).

```kotlin
// From app/src/main/java/com/multispace/platform/PinSecurityManager.kt
fun hashPin(pin: String, saltBase64: String): String {
  require(pin.isNotBlank()) { "PIN cannot be blank" }
  require(saltBase64.isNotBlank()) { "Salt cannot be blank" }
  // ...
}
```

---

### 5.2 Functional Error Handling: `Result<T>` and `runCatching`

Rather than throwing checked or unchecked exceptions across architectural layers, the repository returns Kotlin's built-in `Result<T>`:

```kotlin
// From app/src/main/java/com/multispace/data/repository/RoomSpaceRepository.kt
override suspend fun createSpace(name: String): Result<Space> = runCatching {
  val cleanName = name.trim()
  if (cleanName.isBlank()) {
    throw IllegalArgumentException("Space name cannot be blank")
  }
  val newSpace = Space(
    id = "space_${UUID.randomUUID()}",
    name = cleanName,
    orderIndex = spaceDao.getSpaceCount()
  )
  spaceDao.insertSpace(newSpace.toEntity())
  newSpace
}
```

At the consumption site in the ViewModel:
```kotlin
val result = spaceRepository.createSpace(name)
result.fold(
  onSuccess = { space -> AppLogger.i(AppLogger.Category.LAUNCHER, "Created: ${space.id}") },
  onFailure = { error -> AppLogger.e(AppLogger.Category.LAUNCHER, "Creation failed", error) }
)
```

---

# PART 2: Architecture & Implementation of Multi-Space Launcher

---

## 6. Project Architecture & Layer Relationships

Multi-Space Launcher strictly follows **Clean Architecture** and **MVVM** (Model-View-ViewModel).

```
 ┌────────────────────────────────────────────────────────┐
 │                      Presentation                      │
 │    (Jetpack Compose UI, ViewModels, Themes, Sheets)    │
 └──────────────────────────┬─────────────────────────────┘
                            │ depends on
                            ▼
 ┌────────────────────────────────────────────────────────┐
 │                         Domain                         │
 │      (Pure Kotlin: Models, Repository Interfaces,      │
 │        PlacementCascadeHelper, PageTurnTransformer)    │
 └──────────────────────────▲─────────────────────────────┘
                            │ implements / uses
 ┌──────────────────────────┴─────────────────────────────┐
 │                          Data                          │
 │      (Room Database, Entities, DAOs, Migrations,       │
 │            DataStore Preferences, Repository)          │
 └────────────────────────────────────────────────────────┘
                            │
 ┌──────────────────────────▼─────────────────────────────┐
 │                        Platform                        │
 │       (RoleManager, AppDiscovery, PinSecurity,         │
 │          AccessibilityService, BiometricAuth)          │
 └────────────────────────────────────────────────────────┘
```

### 6.1 Package Directory Breakdown

1. **`com.multispace` (Root)**
   - `MainActivity.kt`: Primary Launcher Activity declaring `CATEGORY_HOME` and `CATEGORY_DEFAULT`.
   - `ConfigurationActivity.kt`: Standalone diagnostics and configuration activity.
2. **`com.multispace.domain`**
   - Pure Kotlin business models (`Space`, `SpaceItemPlacement`, `SpaceFolder`, `SpaceDockItem`, `PageTurnEffect`).
   - Repository contracts (`SpaceRepository.kt`).
   - Domain algorithms (`PlacementCascadeHelper.kt`).
3. **`com.multispace.data`**
   - `entity/`: Room SQLite table definitions (`SpaceEntity`, `SpaceItemPlacementEntity`, etc.).
   - `dao/`: SQLite Data Access Objects with reactive queries (`SpaceDao`, `SpaceLayoutDao`, `SpaceMembershipDao`).
   - `database/`: `LauncherDatabase.kt` defining Room migrations (v1 through v8).
   - `repository/`: `RoomSpaceRepository.kt` implementing the repository contract.
   - `preferences/`: `LauncherPreferences.kt` backed by Jetpack DataStore.
4. **`com.multispace.presentation`**
   - UI Screens (`LauncherHomeScreen`, `Layer1HomeScreen`, `Layer2LibraryScreen`, `MultiSpaceLockScreen`).
   - Customization and Dialogs (`DesktopCustomizationSheet`, `FolderDialog`, `PageTurnEffectConfigView`).
   - ViewModels (`SpaceViewModel`, `AppDiscoveryViewModel`).
   - `widget/`: `DesktopWidgetView.kt` for hosting Android AppWidgets and internal widgets.
5. **`com.multispace.platform`**
   - Android OS integrations (`HomePlatformManager`, `AppDiscoveryManager`, `PinSecurityManager`, `RecentsController`, `MultiSpaceAccessibilityService`).
6. **`com.multispace.diagnostics`**
   - Structured logging engine (`AppLogger.kt`).

---

## 7. Application Startup & Android Lifecycle Management

### 7.1 `MainActivity` as the System Launcher Activity

In `AndroidManifest.xml`, `MainActivity` is declared with:
```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.HOME" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```
This tells the Linux kernel and Android framework that `MainActivity` can act as the primary device home screen.

---

### 7.2 RoleManager and Default Home Detection

On Android 10+ (API 29+), setting the default launcher is managed through Android's `RoleManager`:

```kotlin
// From app/src/main/java/com/multispace/MainActivity.kt
fun requestSetDefaultHome() {
  try {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val roleManager = getSystemService(RoleManager::class.java)
      if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
        val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
        requestHomeRoleLauncher.launch(intent)
        return
      }
    }
    // Fallback to system Home settings for older devices
    HomePlatformManager.openDefaultHomeSettings(this)
  } catch (e: Exception) {
    AppLogger.e(AppLogger.Category.LAUNCHER, "Failed to launch default Home request intent", e)
    HomePlatformManager.openDefaultHomeSettings(this)
  }
}
```

---

### 7.3 Lifecycle Interceptions: `ACTION_SCREEN_OFF` and `onNewIntent`

Launchers have unique lifecycle behaviors:

1. **Screen Off Security Interception:**
   When the physical device screen turns off, the launcher immediately locks any active protected space:
   ```kotlin
   private val screenOffReceiver = object : android.content.BroadcastReceiver() {
     override fun onReceive(context: android.content.Context?, intent: Intent?) {
       if (intent?.action == Intent.ACTION_SCREEN_OFF) {
         spaceViewModel.lockPhone()
       }
     }
   }
   ```

2. **Home Button / Gesture Interception (`onNewIntent`):**
   When the user is in an app and presses the Home button (or swipes up from the navigation bar), Android sends an intent to the existing `MainActivity` instance via `onNewIntent`:
   ```kotlin
   override fun onNewIntent(intent: Intent) {
     super.onNewIntent(intent)
     setIntent(intent)

     val isHomeIntent = intent.hasCategory(Intent.CATEGORY_HOME) ||
       (intent.action == Intent.ACTION_MAIN && intent.categories?.contains(Intent.CATEGORY_HOME) == true)

     if (isHomeIntent) {
       // Return user to Layer 1 (Curated Desk) whenever Home is pressed
       spaceViewModel.setLayer(1)
     }
   }
   ```

---

## 8. Navigation & The Two-Layer Spatial Model

### 8.1 Layer 1 (Curated Desk) vs. Layer 2 (App Library)

Multi-Space Launcher rejects traditional endless drawer navigation in favor of a **Two-Layer Spatial Model**:

```
 ┌───────────────────────────────────────────────────────────┐
 │               Layer 1: The Curated Desktop                │
 │   - Multi-page horizontal pager with 3D turn physics      │
 │   - Multi-cell AppWidgets (System + Internal)             │
 │   - Pinned apps, custom grid layouts, custom folders      │
 │   - Persistent customizable Dock Bar                      │
 └─────────────────────────────▲─────────────────────────────┘
                               │ Swipe Up / Dock App Button
                               ▼
 ┌───────────────────────────────────────────────────────────┐
 │             Layer 2: The Space App Library                │
 │   - Categorized directory of all assigned applications    │
 │   - Instant type-to-filter search bar                     │
 │   - Direct drag-to-Layer-1 shortcut capability            │
 └───────────────────────────────────────────────────────────┘
```

The layer transition is managed by `SpaceViewModel.activeLayerIndex` and smoothly animated in Compose using `AnimatedContent`:
```kotlin
// From app/src/main/java/com/multispace/presentation/LauncherHomeScreen.kt
AnimatedContent(
  targetState = activeLayer,
  transitionSpec = {
    if (targetState == 2) {
      (slideInVertically { height -> height } + fadeIn())
        .togetherWith(slideOutVertically { height -> -height / 3 } + fadeOut())
    } else {
      (slideInVertically { height -> -height / 3 } + fadeIn())
        .togetherWith(slideOutVertically { height -> height } + fadeOut())
    }
  }
) { layer ->
  if (layer == 1) {
    Layer1HomeScreen(/*...*/)
  } else {
    Layer2LibraryScreen(/*...*/)
  }
}
```

---

### 8.2 Security Interception & Lock Screen Transition

In `MainActivity.kt`, the root UI dynamically branches on `isPhoneLocked`:
```kotlin
val isPhoneLocked by spaceViewModel.isPhoneLocked.collectAsState()

if (isPhoneLocked) {
  MultiSpaceLockScreen(
    spaceViewModel = spaceViewModel,
    onUnlockSuccess = { /* Reveal desktop */ }
  )
} else {
  LauncherHomeScreen(/*...*/)
}
```
If a space is protected by a PIN, Pattern, or Biometrics, it cannot be rendered or inspected until authentication succeeds.

---

## 9. Jetpack Compose UI Engine & State Management

### 9.1 Declarative State Hoisting and Recomposition Control

In Jetpack Compose, the UI is a function of state: `UI = f(State)`.

- **State flows down:** The ViewModel provides immutable `StateFlow` instances.
- **Events flow up:** Child Composables emit callbacks (`onLaunchApp`, `onMoveItem`, `onDropItem`).

```kotlin
@Composable
fun Layer1HomeScreen(
  space: Space,
  placements: List<SpaceItemPlacement>,
  dockItems: List<SpaceDockItem>,
  onLaunchApp: (DiscoveredApp) -> Unit,
  onUpdatePlacement: (SpaceItemPlacement) -> Unit
)
```
By making `Layer1HomeScreen` accept plain parameters and lambdas, it becomes:
1. Completely decoupled from ViewModel lifecycles.
2. 100% testable in Compose Previews and Screenshot Tests.

---

### 9.2 Interactive Canvas Rendering (`PatternLockCanvas`)

For pattern-based authentication, Multi-Space Launcher uses low-level Compose `Canvas` drawing rather than standard UI components:

```kotlin
// From app/src/main/java/com/multispace/presentation/PatternLockCanvas.kt
Canvas(
  modifier = modifier
    .fillMaxSize()
    .pointerInput(rows, cols) {
      detectDragGestures(
        onDragStart = { offset ->
          val hitNode = findHitNode(offset, nodePositions, touchRadiusPx)
          if (hitNode != null) {
            selectedNodes.add(hitNode)
            currentTouchPos = offset
          }
        },
        onDrag = { change, _ ->
          change.consume()
          currentTouchPos = change.position
          val hitNode = findHitNode(change.position, nodePositions, touchRadiusPx)
          if (hitNode != null && !selectedNodes.contains(hitNode)) {
            selectedNodes.add(hitNode)
          }
        },
        onDragEnd = {
          onPatternCompleted(selectedNodes.toList())
          selectedNodes.clear()
          currentTouchPos = null
        }
      )
    }
) {
  // 1. Draw connection lines between selected nodes
  for (i in 0 until selectedNodes.size - 1) {
    val p1 = nodePositions[selectedNodes[i]] ?: continue
    val p2 = nodePositions[selectedNodes[i + 1]] ?: continue
    drawLine(
      color = activeColor,
      start = p1,
      end = p2,
      strokeWidth = 6.dp.toPx(),
      cap = StrokeCap.Round
    )
  }

  // 2. Draw live trailing line to finger position
  currentTouchPos?.let { touchPos ->
    val lastPos = nodePositions[selectedNodes.last()]
    if (lastPos != null) {
      drawLine(
        color = activeColor.copy(alpha = 0.6f),
        start = lastPos,
        end = touchPos,
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
      )
    }
  }

  // 3. Draw dot circles
  for ((id, pos) in nodePositions) {
    val isSelected = selectedNodes.contains(id)
    drawCircle(
      color = if (isSelected) activeColor else idleColor,
      radius = if (isSelected) 12.dp.toPx() else 6.dp.toPx(),
      center = pos
    )
  }
}
```

---

### 9.3 Touch Gestures & Pointer Input Subsystem

Drag-and-drop on the desktop requires detecting long-press gestures, tracking pointer movement across grid boundaries, calculating collisions, and handling drop operations:

```kotlin
Modifier.pointerInput(unit) {
  detectDragGesturesAfterLongPress(
    onDragStart = { offset ->
      hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
      draggedItem = item
      currentPointerPos = offset
      isDragging = true
    },
    onDrag = { change, dragAmount ->
      change.consume()
      currentPointerPos += dragAmount
      checkPageEdgeHover(currentPointerPos)
    },
    onDragEnd = {
      resolveDropTarget(draggedItem, currentPointerPos)
      draggedItem = null
      isDragging = false
    },
    onDragCancel = {
      draggedItem = null
      isDragging = false
    }
  )
}
```

---

## 10. Data Persistence: Room Database & DataStore

### 10.1 Room Database, Schema Versions & SQLite Migrations

Multi-Space Launcher stores all configuration in an SQLite database via Android's Room library:

```kotlin
// From app/src/main/java/com/multispace/data/entity/SpaceEntity.kt
@Entity(tableName = "spaces")
data class SpaceEntity(
  @PrimaryKey
  val id: String,
  val name: String,
  @ColumnInfo(name = "order_index")
  val orderIndex: Int,
  @ColumnInfo(name = "created_at")
  val createdAt: Long,
  @ColumnInfo(name = "updated_at")
  val updatedAt: Long,
  @ColumnInfo(name = "auth_policy")
  val authPolicy: String = "NONE",
  @ColumnInfo(name = "pin_salt")
  val pinSalt: String? = null,
  @ColumnInfo(name = "pin_hash")
  val pinHash: String? = null,
  @ColumnInfo(name = "page_turn_effect")
  val pageTurnEffect: String = "NORMAL"
)
```

#### Safe Migrations Across Releases:
Whenever a column is added, Room requires an explicit `Migration` object to alter the schema safely without data loss:

```kotlin
// From app/src/main/java/com/multispace/data/database/LauncherDatabase.kt
private val MIGRATION_1_2 = object : Migration(1, 2) {
  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("ALTER TABLE spaces ADD COLUMN background_type TEXT NOT NULL DEFAULT 'DEFAULT'")
    db.execSQL("ALTER TABLE spaces ADD COLUMN background_color INTEGER DEFAULT NULL")
    db.execSQL("ALTER TABLE spaces ADD COLUMN background_image_uri TEXT DEFAULT NULL")
  }
}
```

---

### 10.2 DAOs, Transactions & Live SQLite Query Streams

Room converts SQL queries into reactive `Flow` instances that emit new data whenever relevant tables are modified:

```kotlin
// From app/src/main/java/com/multispace/data/dao/SpaceDao.kt
@Dao
interface SpaceDao {
  @Query("SELECT * FROM spaces ORDER BY order_index ASC")
  fun getAllSpacesFlow(): Flow<List<SpaceEntity>>

  @Query("SELECT * FROM spaces WHERE id = :spaceId LIMIT 1")
  suspend fun getSpaceById(spaceId: String): SpaceEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertSpace(space: SpaceEntity): Long

  @Query("DELETE FROM spaces WHERE id = :spaceId")
  suspend fun deleteSpaceById(spaceId: String): Int
}
```

In `SpaceLayoutDao.kt`, operations updating multiple rows run atomically inside a `@Transaction`:
```kotlin
@Transaction
suspend fun replacePlacementsForSpace(spaceId: String, layer: String, placements: List<SpaceItemPlacementEntity>) {
  deletePlacementsForSpaceLayer(spaceId, layer)
  insertPlacements(placements)
}
```

---

### 10.3 Entity to Domain Model Mapping

The database schema is strictly isolated from business logic. Extension functions in `SpaceEntity.kt` perform bidirectional mapping:

```kotlin
fun SpaceEntity.toDomain(): Space = /* ... */
fun Space.toEntity(): SpaceEntity = /* ... */
```

This ensures that if the database schema evolves, only the mapping functions and DAOs need updating; the domain model, ViewModels, and UI remain unaffected.

---

### 10.4 Reactive Preferences with Jetpack DataStore

For lightweight key-value settings (e.g., active Space ID), Multi-Space Launcher uses **Preferences DataStore** rather than legacy `SharedPreferences`:

```kotlin
// From app/src/main/java/com/multispace/data/preferences/LauncherPreferences.kt
class LauncherPreferences(private val context: Context) {
  private val Context.dataStore by preferencesDataStore(name = "launcher_prefs")

  val activeSpaceIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
    prefs[KEY_ACTIVE_SPACE_ID]
  }

  suspend fun setActiveSpaceId(spaceId: String) {
    context.dataStore.edit { prefs ->
      prefs[KEY_ACTIVE_SPACE_ID] = spaceId
    }
  }

  companion object {
    private val KEY_ACTIVE_SPACE_ID = stringPreferencesKey("active_space_id")
  }
}
```
DataStore guarantees non-blocking, asynchronous execution on `Dispatchers.IO` and transactional disk writes.

---

## 11. Subsystems & Algorithmic Highlights

### 11.1 Spatial Footprint & Ripple Displacement (`PlacementCascadeHelper`)

A core technical challenge in modern launchers is the **shared spatial grid**: single-cell apps and multi-cell widgets (`2×2`, `4×2`) coexist on the same screen. When an item is dropped onto an occupied spot, displaced items must ripple forward smoothly across pages without overlapping or leaving orphaned slots.

```
 ┌───┬───┬───┬───┐          ┌───┬───┬───┬───┐
 │ A │ B │ C │ D │          │ A │ W │ W │ B │  <- B and C are displaced!
 ├───┼───┼───┼───┤  Drop W  ├───┼───┼───┼───┤
 │ E │ F │ G │ H │ ───────> │ E │ W │ W │ C │  <- W takes 2x2 footprint
 ├───┼───┼───┼───┤  (2x2)   ├───┼───┼───┼───┤
 │ I │ J │ K │ L │          │ F │ G │ H │ D │  <- Items ripple forward
 └───┴───┴───┴───┘          └───┴───┴───┴───┘
```

#### The Spatial Footprint:
```kotlin
// From app/src/main/java/com/multispace/domain/model/PlacementCascadeHelper.kt
data class Footprint(
  val pageIndex: Int,
  val row: Int,
  val col: Int,
  val spanX: Int,
  val spanY: Int,
  val cols: Int,
  val pageSize: Int
) {
  val globalSlots: Set<Int> by lazy {
    val slots = mutableSetOf<Int>()
    for (dr in 0 until spanY) {
      for (dc in 0 until spanX) {
        val r = row + dr
        val c = col + dc
        slots.add(pageIndex * pageSize + (r * cols + c))
      }
    }
    slots
  }

  fun intersects(other: Footprint): Boolean {
    if (pageIndex != other.pageIndex) return false
    val overlapX = maxOf(col, other.col) < minOf(col + spanX, other.col + other.spanX)
    val overlapY = maxOf(row, other.row) < minOf(row + spanY, other.row + other.spanY)
    return overlapX && overlapY
  }
}
```

#### The Ripple Displacement Algorithm:
1. Calculate the incoming item's `Footprint` at `(targetPage, targetPosition)`.
2. Find all existing items whose footprints intersect the incoming footprint.
3. Remove colliding items from the active grid and queue them in a displacement list.
4. Place the incoming item at the requested target location.
5. Iterate through displaced items in order of original position:
   - For each displaced item, search forward for the next available slot where its `Footprint` fits without intersecting already placed items or grid boundaries.
   - If a page fills up, increment `pageIndex` and cascade to subsequent pages.

---

### 11.2 3D Page Turn Transitions (`PageTurnTransformer`)

To deliver tactile, responsive visual page transitions, Multi-Space Launcher computes real-time 3D transformation matrices based on the pager's scroll offset:

```kotlin
// From app/src/main/java/com/multispace/presentation/PageTurnTransformer.kt
fun calculatePageTransformation(
  effect: PageTurnEffect,
  pageOffset: Float,
  pageWidth: Float = 1080f,
  intensity: Float = 1.0f
): PageTransformation {
  if (effect == PageTurnEffect.NORMAL) return PageTransformation()

  val clampedOffset = pageOffset.coerceIn(-1f, 1f)
  val absOffset = abs(clampedOffset)
  if (absOffset < 0.0001f) return PageTransformation()

  val safeIntensity = intensity.coerceIn(0.5f, 2.0f)

  return when (effect) {
    PageTurnEffect.NORMAL -> PageTransformation()

    PageTurnEffect.CUBE -> {
      // 3D Cube:
      // When pageOffset > 0, page is moving to the left: pivot on right edge (1.0f)
      // When pageOffset < 0, page is moving to the right: pivot on left edge (0.0f)
      val rotationY = 90f * clampedOffset * safeIntensity
      val pivotX = if (clampedOffset > 0) 1.0f else 0.0f
      PageTransformation(
        translationX = 0f,
        rotationY = rotationY,
        transformOriginX = pivotX,
        transformOriginY = 0.5f,
        cameraDistanceMultiplier = 8000f,
        alpha = 1f - (absOffset * 0.2f)
      )
    }

    PageTurnEffect.WINDMILL -> {
      // Windmill: Pivots around bottom-center with Z-axis rotation
      val rotationZ = -45f * clampedOffset * safeIntensity
      PageTransformation(
        rotationZ = rotationZ,
        transformOriginX = 0.5f,
        transformOriginY = 1.0f,
        alpha = 1f - (absOffset * 0.3f)
      )
    }

    PageTurnEffect.ZOOM -> {
      // Zoom: Shrinks outgoing page into depth
      val scale = (1f - absOffset * 0.4f * safeIntensity).coerceIn(0.4f, 1f)
      PageTransformation(
        scaleX = scale,
        scaleY = scale,
        alpha = 1f - absOffset * 0.5f
      )
    }

    PageTurnEffect.CROSSFADE -> {
      // Crossfade: Neutralizes translation to dissolve in place
      PageTransformation(
        translationX = -clampedOffset * pageWidth,
        alpha = (1f - absOffset * safeIntensity).coerceIn(0f, 1f)
      )
    }
  }
}
```

---

### 11.3 Desktop Widgets & Dynamic Resizing Subsystem

Multi-Space Launcher supports both native Android system widgets (via `AppWidgetHost`) and built-in launcher widgets (Clock, Search, Calendar, Battery, Notes) in `DesktopWidgetView.kt`.

When entering customization mode:
1. Resizing handles appear at the corners and edges of the widget.
2. Dragging a handle dynamically computes new `(spanX, spanY)` dimensions constrained by grid limits.
3. Placement validation prevents resizing into occupied cells unless displacement is triggered.

---

### 11.4 Security Subsystem: PBKDF2 Hashing & Salted Validation

User privacy across Spaces is backed by cryptographic standards:
- **Algorithm:** `PBKDF2WithHmacSHA256`
- **Iterations:** 10,000 rounds
- **Key Length:** 256 bits
- **Salt:** Cryptographically secure 16-byte random salt generated via `SecureRandom` per space.

```kotlin
// From app/src/main/java/com/multispace/platform/PinSecurityManager.kt
fun hashPin(pin: String, saltBase64: String): String {
  require(pin.isNotBlank()) { "PIN cannot be blank" }
  require(saltBase64.isNotBlank()) { "Salt cannot be blank" }

  val salt = Base64.getDecoder().decode(saltBase64)
  val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
  val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
  val hash = factory.generateSecret(spec).encoded
  return Base64.getEncoder().encodeToString(hash)
}

fun verifyPin(enteredPin: String, saltBase64: String?, expectedHashBase64: String?): Boolean {
  if (saltBase64.isNullOrEmpty() || expectedHashBase64.isNullOrEmpty()) return false
  return try {
    val computedHash = hashPin(enteredPin, saltBase64)
    val computedBytes = Base64.getDecoder().decode(computedHash)
    val expectedBytes = Base64.getDecoder().decode(expectedHashBase64)
    // Constant-time comparison prevents side-channel timing attacks
    MessageDigest.isEqual(computedBytes, expectedBytes)
  } catch (e: Exception) {
    false
  }
}
```

---

## 12. Testing Architecture & Verification

### 12.1 Pure JVM Unit Testing for Critical Business Logic

Because Android instrumented tests require physical devices or emulators and take minutes to execute, the architecture isolates pure algorithms into JVM testable classes.

By extracting spatial calculations into `PlacementCascadeHelper` and mathematical 3D transformations into `PageTurnTransformer`, the core logic runs in standard JUnit unit tests in milliseconds:

```
app/src/test/java/com/multispace/
├── PlacementCascadeTest.kt                 # Grid footprint, ripple shifts, drops
├── PageTurnEffectTest.kt                   # 3D math, pivots, rotations, alphas
├── DesktopCustomizationTest.kt             # Domain models, wallpaper configs
├── DockBarDragAndDropAndDeduplicationTest.kt # Dock reordering, capacity limits
├── CrossPagePlacementTest.kt               # Multi-page boundary cascading
└── SpaceCredentialMatchingTest.kt          # PIN and pattern verification
```

---

### 12.2 Test Structure & Assertions in Real Tests

Here is how `PlacementCascadeTest.kt` verifies ripple displacement:

```kotlin
// From app/src/test/java/com/multispace/PlacementCascadeTest.kt
@Test
fun testDropOnOccupiedSlotShiftsOccupyingAppToNextPosition() {
  val existing = listOf(
    SpaceItemPlacement(id = "app1", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app1"),
    SpaceItemPlacement(id = "app2", spaceId = "s1", pageIndex = 0, positionIndex = 1, packageName = "com.app2")
  )
  val itemToInsert = SpaceItemPlacement(id = "app3", spaceId = "s1", pageIndex = 0, positionIndex = 0, packageName = "com.app3")

  // Dropping app3 at pos 0 must displace app1 to pos 1, and app2 to pos 2
  val result = PlacementCascadeHelper.computeFullPlacementsAfterDrop(
    allCurrentPlacements = existing,
    itemToInsert = itemToInsert,
    targetPage = 0,
    targetPosition = 0,
    pageSize = 4
  )

  val map = result.associateBy { it.id }
  assertEquals(0, map["app3"]?.positionIndex)
  assertEquals(1, map["app1"]?.positionIndex)
  assertEquals(2, map["app2"]?.positionIndex)
}
```

These deterministic unit tests provide instant confidence that core desktop placement and mathematics work reliably without edge cases.

---

# Summary Cheat Sheet: Kotlin Idioms in Multi-Space Launcher

| Kotlin Feature | Example in Multi-Space Launcher | Why It Matters |
|---|---|---|
| `val` immutability | `val activeSpace: StateFlow<Space?>` | Thread safety, predictable state |
| Data Class `copy()` | `item.copy(pageIndex = targetPage)` | Non-destructive state mutation |
| Extension Functions | `SpaceEntity.toDomain()`, `Modifier.pageTurnEffect()` | Decoupled architecture, clean DSLs |
| `when` as Expression | `when (effect) { PageTurnEffect.CUBE -> ... }` | Compile-time exhaustiveness |
| Scope Functions | `Intent(...).apply { flags = ... }` | Fluent initialization, concise code |
| Sealed Types / Enums | `PageTurnEffect`, `LauncherUiState` | Type-safe state modeling |
| Flow / StateFlow | `combine(...)`, `flatMapLatest { ... }` | Reactive unidirectional data flow |
| Preconditions | `require(pin.isNotBlank())` | Fail-fast invariant verification |
| Clean Architecture | Domain $\leftarrow$ Data $\leftarrow$ Presentation | Decoupled, modular, highly testable |

---

*Multi-Space Launcher — Crafted with Modern Kotlin & Jetpack Compose.*
