# Contributing to HealthCoach

Thank you for contributing to **HealthCoach**! HealthCoach is a local-first personal health and wellness analytics application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform** targeting Desktop (JVM) and Android.

These guidelines exist to maximize development efficiency, maintain high code quality, protect user privacy and safety, and ensure smooth collaboration between contributors and maintainers.

---

## Core Principles & Non-Negotiables

To maximize user well-being and data security, every contribution must uphold these core principles:

1. **User Data Sovereignty & Absolute Privacy**:
   - All personal metrics (weight, blood pressure, food journal, fasting) must be stored locally in SQLite via SqlDelight.
   - **Zero Telemetry / Zero Tracking**: Never introduce analytics SDKs, tracking beacons, diagnostic phone-home routines, or unprompted remote network calls.
   - Remote synchronization is strictly opt-in by the user (local folder export/import, LAN peer-to-peer pairing, or personal Google Drive `appDataFolder` sandbox).

2. **Medical Disclaimers & Compliance**:
   - HealthCoach is a personal wellness tool, not a certified medical device.
   - Any UI feature introducing or modifying clinical metric interpretations (such as AHA blood pressure stages, BMI categories, or Mifflin-St Jeor BMR/TDEE calculations) must reference standard educational guidelines and retain clear user disclaimers (`DISCLAIMER.md` / `AppInfo.MEDICAL_DISCLAIMER`).

3. **Multiplatform Architecture & Modularity**:
   - Shared business logic, database queries, ViewModels, and UI composables reside in `shared/src/commonMain`.
   - Platform-specific capabilities reside in `androidMain`, `jvmMain`, `androidApp`, or `desktopApp`.

---

## Getting Started

### Prerequisites
- **JDK 17** or higher (Eclipse Temurin recommended)
- **Android SDK** (API Level 34+; required for Android target builds)
- Optional for Linux packaging: `flatpak` and `flatpak-builder`

### Building and Running Locally

- **Launch Desktop Application**:
  ```bash
  ./gradlew :desktopApp:run
  ```

- **Hot Reload Development (Desktop UI)**:
  ```bash
  ./gradlew :desktopApp:hotRun --auto
  ```

- **Assemble & Install Android Debug App**:
  ```bash
  ./gradlew :androidApp:installDebug
  ```

---

## Development Standards

### 1. Database Changes & Migrations
- Schema definitions reside in `shared/src/commonMain/sqldelight/com/lbthomas/healthcoach/db/`.
- Never modify existing released `.sq` schema tables directly without providing an incremental `.sqm` migration file in `shared/src/commonMain/sqldelight/databases/`.
- Verify migration integrity with `verifyMigrations = true` in Gradle.

### 2. Architecture & UI Guidelines
- **UI Framework**: Compose Multiplatform with Material 3 theming.
- **State Management**: StateFlow and Coroutines exposed by Koin-injected ViewModels.
- **Layout Adaptability**: Support both compact single-pane mobile layouts and wide dual-pane desktop layouts.

### 3. Conventional Commits & Versioning
All Git commit messages and PR titles must strictly conform to the **Conventional Commits** specification:

- `feat(scope)`: A new user-facing feature (triggers MINOR version bump).
- `fix(scope)`: A bug fix (triggers PATCH version bump).
- `feat!:` / `fix!:` / `BREAKING CHANGE:`: Breaking change (triggers MAJOR version bump).
- `refactor(scope)`: Code restructuring without behavior changes.
- `test(scope)`: Adding or updating unit or integration tests.
- `docs(scope)`: Documentation updates only.
- `chore(scope)`: Maintenance, build configuration, or dependency updates.

*Examples:*
- `feat(sync): add dynamic peer resolution fallback`
- `fix(journal): resolve FAB occlusion on compact screens`
- `test(settings): add P2P dynamic discovery unit tests`

---

## Pull Request & Quality Assurance Process

To minimize review turnaround time and eliminate regressions:

1. **Run Local Verification**:
   Execute the full verification suite before pushing:
   ```bash
   ./gradlew check test
   ```
2. **Branch Hygiene**:
   - Create focused branches off `main` (e.g. `feature/p2p-dynamic-resolution` or `fix/journal-fab-padding`).
   - Keep pull requests atomic and focused on a single concern to streamline review and minimize merge conflicts.
3. **Continuous Integration (CI)**:
   - Ensure all automated checks pass on GitHub Actions (`pr-validation.yml`).

---

## Reporting Issues & Proposing Features

- **Bug Reports**: Provide platform details (OS / Android version), reproduction steps, expected vs. actual behavior, and relevant logs (`healthcoach.log` on Desktop or `adb logcat` on Android).
- **Feature Proposals**: Open an issue detailing the motivation, user benefit, and architectural impact before beginning major implementations.
