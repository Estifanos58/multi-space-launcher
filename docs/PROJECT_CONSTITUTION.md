# Multi-Space Android Launcher — Project Constitution

## 1. Project Identity
The **Multi-Space Android Launcher** is a single-module, local-first third-party Android home-screen application. It allows a single physical Android device to define and switch between contextual launcher environments called **Spaces**.

## 2. V1 Boundary
V1 provides **launcher-level organization and presentation filtering**, NOT operating-system-level application isolation.
* A Space controls what applications and layouts the launcher UI presents.
* A Space may require local numeric PIN authentication before its presentation is rendered.
* Switching Spaces changes the launcher mode, not the Android user account, device profile, or installed package state.

## 3. V1 Non-Goals
The following are strictly outside the V1 product scope:
* Android Multi-User management, Work Profiles, Managed Profiles, or Device Owner controls.
* Operating-system-level application isolation, filesystem separation, or per-Space data sandboxing.
* Root, custom ROM, boot image modification, or privileged system APK modification.
* Accessibility Service or System Overlay based app restriction / simulation hacks.
* Cloud backends, remote servers, user account sync, or telemetry/analytics infrastructure.
* Widget hosting (`AppWidgetHost`), custom icon pack engines, or complex animation frameworks.
* Automated application test code (e.g. Robolectric, Espresso, UI tests).

## 4. Architecture Principles
* **Single-Module Architecture:** The project is organized as a single application module (`app`) with explicit conceptual package boundaries: `presentation`, `domain`, `data`, `platform`, and `diagnostics`.
* **Platform Authority:** Android owns installed packages and runtime identities. The launcher references stable activity/component identities and persists user preferences and membership.
* **Separation of Durable and Ephemeral State:** Durable configuration (Spaces, memberships, layouts) lives in Room/DataStore; ephemeral state (loading, focus, active UI transitions) lives in ViewModel/Compose runtime.

## 5. Repository Evidence Rule
Repository evidence outranks AI memory and conversational assumptions.
* Facts about code, dependencies, and configuration must be verified by direct repository inspection.
* Approved architecture describes intent; the repository describes current implementation reality.

## 6. Physical Testing Rule
**Build success does NOT equal physical device verification.**
* A passing Gradle build (`BUILDS`) only proves compilation and packaging.
* Runtime behavior, launcher eligibility, Home button capture, and lifecycle stability require human-confirmed validation on physical Android hardware recorded in `PHYSICAL_TEST_LOG.md`.
* Only a human tester may issue a `PASS` rating.

## 7. No Automated Test-Code Rule
Automated application test code (unit, integration, or UI test suites) is explicitly prohibited for V1. Verification relies on build compilation and structured human physical testing on real hardware.

## 8. Security & Privacy Boundaries
* The launcher makes zero claims of OS-level security or application hiding outside its own UI surface.
* PIN credentials must never be stored or logged in plaintext; cryptographic protection via platform facilities (such as Android Keystore) is required.
* Application metadata and Space configurations remain strictly local to the device private storage.

## 9. Scope-Control Rules
* **No Speculative Implementation:** Features must not be partially implemented ahead of their scheduled development phase.
* **Structural Placeholders vs Features:** A package marker or interface placeholder is permissible; functional implementation before its gate is forbidden.
* New ideas outside the active task must be categorized (e.g. `FUTURE`, `V1 OPTIONAL`, `TECHNICAL DEBT`) rather than built.

## 10. Dependency Rules
Dependencies must remain minimal. No library may be introduced without a concrete, documented requirement for the current active phase. Large frameworks (DI engines, networking, Firebase, analytics) are strictly prohibited for V1 core.

## 11. Agent Behavior Rules
* AI agents are replaceable workers; persistent project memory resides in repository documentation.
* Discovery before modification: inspect existing repository structure and read continuity documents before editing.
* Preserve uncertainty: use `UNKNOWN` when evidence is missing rather than fabricating facts.

## 12. Uncertainty Rules
When information cannot be established from repository inspection or physical test logs, it must be marked `UNKNOWN`. It is forbidden to assume behavior across Android OEMs or versions without evidence.

## 13. Documentation Rules
* Documentation must be concise, accurate, and reflect actual repository state.
* The `/docs/` directory contains exactly the nine canonical continuity documents. No arbitrary documentation sub-trees should be created without justification.

## 14. Definition-of-Done Principles
A phase or task is done only when:
1. Code complies with the architecture and scope boundary.
2. The debug APK builds cleanly with zero errors.
3. Relevant continuity documents are updated.
4. Physical testing status is accurately recorded (or marked `NOT PERFORMED` where hardware testing has not yet occurred).
