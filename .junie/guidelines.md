# HealthCoach Agent Guidelines & Engineering Standards

These guidelines govern coding agents and contributors modifying the HealthCoach codebase.

## 1. Conventional Commits & Versioning

All Git commit messages and PR titles must strictly conform to the **Conventional Commits** specification (`<type>(<optional scope>): <description>`):

* **`feat`**: A new user-facing feature (triggers **MINOR** SemVer bump, e.g., `0.9.1` -> `0.10.0`).
* **`fix`**: A bug fix (triggers **PATCH** SemVer bump, e.g., `0.9.1` -> `0.9.2`).
* **`feat!:`** or **`fix!:`** or footer `BREAKING CHANGE:`: Breaking change (triggers **MAJOR** SemVer bump, e.g., `0.9.1` -> `1.0.0`).
* **`chore`**: Maintenance, build config, dependency bumps, tooling (no version bump).
* **`docs`**: Documentation updates only (no version bump).
* **`refactor`**: Code restructuring with no behavior change (no version bump).
* **`test`**: Adding or updating tests only (no version bump).
* **`ci`**: GitHub Actions or CI/CD workflow updates (no version bump).
* **`perf`**: Performance improvement (triggers **PATCH** SemVer bump).

Examples:
* `feat(sync): add companion client discovery notice in sync tab`
* `fix(bloodpressure): handle null pulse readings in chart points`
* `chore(deps): update ktorVersion to 3.1.1`

## 2. User Data Sovereignty & Offline Independence

HealthCoach is architected around strict local-first data ownership:
* All health metrics (weight, blood pressure, food journal, fasting) must be stored in the local SQLite database via SqlDelight.
* **Zero Telemetry / Zero Tracking**: Never introduce tracking SDKs, analytical beaconing, or unprompted remote network calls.
* Remote synchronization must remain strictly opt-in by the user (local export/import, LAN peer-to-peer sync, or private Google Drive `appDataFolder` sandbox).

## 3. Medical & Compliance Disclaimers

HealthCoach is personal wellness software and not a certified medical device.
* Any UI surface introducing new clinical metric interpretations (blood pressure classifications, BMI, BMR/TDEE calculations) must reference general educational guidelines and retain clear user disclaimers.
* Core medical disclaimer text resides in `DISCLAIMER.md` and `AppInfo.MEDICAL_DISCLAIMER`.

## 4. Multiplatform Architecture

* The `shared` module uses Kotlin Multiplatform (KMP) targeting JVM (Desktop) and Android.
* Shared business logic, view models, and Compose Multiplatform UI components reside in `shared/src/commonMain`.
* Platform-specific capabilities (e.g. Android Activity/KeyStore or Desktop Swing/System Tray) reside in the respective `androidApp` or `desktopApp` modules or platform source sets (`androidMain`, `jvmMain`).

## 5. Verification & Code Quality

Before concluding any task or submitting changes:
* Run `./gradlew check` to verify unit tests, JVM tests, Android linting, and SqlDelight migration schemas (`verifyMigrations = true`).
* Maintain clean formatting and consistent naming idioms mirroring surrounding Kotlin Multiplatform code.
