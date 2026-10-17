## Summary of Changes

<!-- Briefly describe the problem solved or feature implemented and its practical outcome. -->

- **Issue Reference**: Closes # <!-- e.g., Closes #21 -->
- **Type of Change**:
  - [ ] `feat`: New user-facing feature (Minor version bump)
  - [ ] `fix`: Bug fix (Patch version bump)
  - [ ] `perf`: Performance improvement (Patch version bump)
  - [ ] `refactor`: Restructuring without behavior change
  - [ ] `test`: New or updated tests
  - [ ] `docs`: Documentation updates
  - [ ] `chore`: Maintenance, dependencies, or build config
  - [ ] `BREAKING CHANGE`: Breaking change (Major version bump)

---

## Architectural & Compliance Checklist

<!-- Review and check each box to confirm that your changes comply with all core guarantees: -->

- [ ] **User Data Sovereignty & Offline Independence**:
  - [ ] All health metrics remain stored locally in SQLite (`SqlDelight`).
  - [ ] No telemetry, analytics beacons, or unprompted remote network calls are introduced.
  - [ ] Remote synchronization (P2P LAN / Google Drive) remains strictly opt-in.
- [ ] **Medical Disclaimer Compliance**:
  - [ ] Any new clinical/health metric interpretations include educational references and retain user disclaimers (`AppInfo.MEDICAL_DISCLAIMER`).
- [ ] **Database & Migrations**:
  - [ ] Existing released schemas were not modified without adding an incremental `.sqm` migration in `databases/`.
  - [ ] Schema migrations pass `verifyMigrations = true`.
- [ ] **Multiplatform Architecture**:
  - [ ] Shared logic resides in `shared/src/commonMain`.
  - [ ] Platform-specific code is confined to `jvmMain`, `androidMain`, `desktopApp`, or `androidApp`.

---

## Verification & Testing

<!-- Detail what verification steps were run to maximize reliability and prevent regressions. -->

- **Automated Verification**:
  - [ ] `./gradlew check test` passed locally with zero failures.
  - [ ] Android lint checks and SqlDelight verification succeeded.
- **Manual / Platform Testing**:
  - [ ] Desktop JVM tested (`./gradlew :desktopApp:run`).
  - [ ] Android tested (Device or Emulator via `./gradlew :androidApp:installDebug`).
  - [ ] Responsive layout verified across compact (mobile) and wide (desktop) screen widths.

---

## Screenshots / Screen Recordings (If Applicable)

<!-- Attach images or GIFs demonstrating UI changes on desktop and/or mobile layouts. -->
