# Current Task — Phase 4: Minimal Usable Launcher

## Task Information
* **Task:** Phase 4 — Minimal Usable Launcher
* **Objective:** Turn the verified launcher shell into a simple, usable Home-screen experience that displays and launches the applications discovered by Phase 2 and launched by Phase 3.
* **Why:** A launcher must provide a real, functional Home-screen surface with an intuitive icon grid, clear labels, reliable tap-to-launch interactions, and stable Home return before introducing Space segmentation (Phase 5+) or authentication (Phase 8).
* **Phase:** Phase 4 — Minimal Usable Launcher
* **Milestone:** Milestone 4 — Usable Home Surface & Core Launcher Loop

---

## Prerequisites
* Phase 0 — Project Foundation (`COMPLETE & VERIFIED`).
* Phase 1 — Launcher Viability Spike (`COMPLETE & PHYSICALLY VERIFIED`).
* Phase 2 — Application Discovery & LauncherApps Integration (`COMPLETE & PHYSICALLY VERIFIED`).
* Phase 3 — Application Launching Viability Spike (`COMPLETE & PHYSICALLY VERIFIED`).

---

## Known Facts
* `LauncherHomeScreen` acts as the primary Home surface rendering an adaptive 4-column application grid.
* Discovered applications are displayed with high-contrast labels and cached icons via `AppDiscoveryManager`.
* Tapping an application invokes `AppLaunchManager.launchApp()` via `AppDiscoveryViewModel`.
* `MainActivity` receives `onNewIntent` and smoothly restores the primary Home surface upon Home button presses.
* Dynamic package monitoring updates the catalog in real-time when apps are added, removed, or updated.
* Loading state, empty state, and error states are cleanly handled without crashing.
* Configuration and system diagnostics are accessible via a clean modal sheet rather than cluttering the Home surface.

---

## Unknown Facts
* Long-term scrolling performance under 300+ installed packages on resource-constrained physical devices (`PENDING PHYSICAL TEST`).
* Visual appearance and grid density across varied physical screen aspect ratios (`PENDING PHYSICAL TEST`).

---

## Conceptual Scope
* Launcher Home surface layout with Material 3 Clean Utility / Minimal theming.
* 4-column application grid with icons and labels.
* Deterministic alphabetical ordering.
* Direct integration with verified `AppLaunchManager` for launch dispatching.
* Graceful loading, empty, and error state handling.
* Clean configuration entry point with default Home status check and diagnostics routing.

---

## Protected Scope (STRICTLY PROHIBITED IN THIS TASK)
* No Spaces, Space creation, or Space switching (Phases 5 & 6).
* No Space membership models or repositories (Phase 5).
* No Room database entities, DAOs, or migrations (Phase 5).
* No DataStore-based Space persistence (Phase 5).
* No PIN or authentication logic (Phase 8).
* No wallpaper or layout customization per Space (Phase 9).
* No widgets, custom icon packs, drag-and-drop, or multi-page desktop management.
* No automated test code (Robolectric, Espresso) per Constitution Rule 7.

---

## Acceptance Criteria
1. Launcher presents a real basic Home surface (`LauncherHomeScreen`) with a 4-column application grid.
2. Discovered applications are displayed with clear labels and cached icons.
3. Tapping an application launches the app via verified `AppLaunchManager`.
4. Home key returns cleanly from external applications to `LauncherHomeScreen`.
5. Dynamic package changes are automatically reflected on the Home grid.
6. Empty, loading, and error states are gracefully handled with zero crashes.
7. Diagnostics/telemetry is cleanly separated into a dedicated view accessible via configuration.
8. Debug APK builds cleanly with 0 errors.

---

## Expected End State
Successful compilation and documentation of Phase 4 ready for physical hardware verification (TEST-004: Tests A through G), followed by Phase 5 (Space Domain + Persistence).


