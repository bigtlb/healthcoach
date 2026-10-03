# HealthCoach Release, CI/CD & Distribution Roadmap

This document outlines the operational roadmap for HealthCoach as it transitions to automated continuous integration, semantic versioning, multi-platform packaging (Windows, Linux, Android), public release distribution via GitHub Pages, and store deployments (Google Play Store and Flathub).

It is structured to provide:
1. **GitHub Contributors & Users**: Clear visibility into how HealthCoach builds, tests, packages, versions, and publishes releases while preserving strict local-first data sovereignty.
2. **Project Maintainer**: Explicit, step-by-step manual action items and configuration checklists required across Google Cloud, GitHub settings, Google Play Console, and Flathub.
3. **Automated Plan Reference**: Implementation details are tracked in the Junie delivery plan at `.junie/plans/cicd-packaging-and-store-distribution.md`.

---

## High-Level Architecture & Release Principles

* **User Data Sovereignty & Offline Independence**: All health metrics remain stored in an embedded SQLite database on the user's device with zero telemetry. Synchronization only occurs to endpoints chosen by the user (local folders, peer-to-peer Wi-Fi LAN, or private Google Drive `appDataFolder` sandbox).
* **Conventional Commits & Semantic Versioning (SemVer)**: Commit messages follow Conventional Commits (`feat:`, `fix:`, `perf:`, `chore:`). Automated workflows analyze commit history, generate changelogs, tag Git releases (`vMAJOR.MINOR.PATCH`), and inject the active version directly into Gradle builds and the in-app **About** screen.
* **Seamless Multi-Platform Desktop & Mobile Packaging**:
  - **Windows**: Bundles a standalone installer (`HealthCoach.msi`) containing an embedded Java runtime (via `jlink`/`jpackage`) and native SQLite DLLs. No external Java or driver installations required.
  - **Linux**: Distributed as a sandboxed **Flatpak** on Flathub, alongside standalone `.deb` and `.rpm` packages.
  - **Android**: Distributed as direct `.apk` downloads on GitHub and as signed Android App Bundles (`.aab`) on Google Play.
* **Unified OAuth Multi-Certificate Strategy**: Google Cloud Console is configured with three Android OAuth Client IDs for the same package (`com.lbthomas.healthcoach`) matching the local debug keystore, CI release keystore, and Google Play App Signing key certificate. This avoids device lock-in and allows Google Drive sync to function identically in local development, direct APK releases, and Google Play installs.
* **Public Landing Page & Client Discovery**: A static GitHub Pages site (`https://bigtlb.github.io/healthcoach/`) provides direct binary download buttons, full release history with categorized Conventional Commit changelogs, legal/privacy policies, and a web destination linked from within the desktop and mobile apps to facilitate peer-to-peer companion setup.

---

## Phase-by-Phase Roadmap & Action Items

### Phase 0: Medical & Health Disclaimer & Repository Badges

#### What's Happening
HealthCoach is personal wellness software. To comply with app store guidelines (Google Play Health Apps Policy, Apple App Store, and open source safety standards), clear health disclaimers are added across all user touchpoints (repository, app About screen, web landing page). Dynamic shields are also added to the top of `README.md` to display live CI build status and the latest release tag.

#### Maintainer Action Items
- [x] Review and approve the standard medical disclaimer text:
  > **Medical & Health Disclaimer**
  > 
  > *HealthCoach is designed and provided solely for personal health tracking, wellness organization, and informational purposes. HealthCoach is not a certified medical device and does not provide clinical diagnoses, medical advice, treatment plans, or emergency health intervention.*
  > 
  > *The clinical classifications (such as American Heart Association blood pressure categories) and metabolic calculations (such as Mifflin-St Jeor BMR and TDEE estimates) provided in this application are general educational references only. Always consult a qualified physician or licensed healthcare professional before making health, dietary, exercise, or medical decisions, or if you have concerns regarding your blood pressure readings, heart rate, or body weight.*

#### Implementation Deliverables
- [x] Create `DISCLAIMER.md` in repository root.
- [x] Add Medical Disclaimer section and dynamic CI/Release badges to `README.md`.
- [x] Add `AppInfo.MEDICAL_DISCLAIMER` in `AppInfo.kt` and render disclaimer card in `AboutTab.kt`.

---

### Phase 1: Google Cloud & Google Drive OAuth Setup

#### What's Happening
HealthCoach supports optional backup and sync via Google Drive's isolated `appDataFolder` sandbox. To enable this across all development and release channels, Google Cloud Console credentials must be configured.

#### Maintainer Action Items
- [ ] **Google Cloud Project**: Go to [Google Cloud Console](https://console.cloud.google.com/), create or open project `HealthCoach Sync`.
- [ ] **Enable Drive API**: In **APIs & Services** → **Library**, enable **Google Drive API**.
- [ ] **OAuth Consent Screen**:
  - Set user type to **External**, App name to `HealthCoach`, add support email.
  - Add scope: `https://www.googleapis.com/auth/drive.appdata` (isolated app storage).
  - Add personal Google account to **Test Users**.
- [ ] **Generate Desktop OAuth Client ID**:
  - In **Credentials** → **Create Credentials** → **OAuth client ID** → **Desktop app**.
  - Save `Client ID` and `Client Secret`.
- [ ] **Generate Android OAuth Client ID (Local Debug)**:
  - Obtain local debug SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android` (or `./gradlew :androidApp:signingReport`).
  - Create Android OAuth Client ID with package `com.lbthomas.healthcoach` and the debug SHA-1.
- [ ] **Generate Production Release Keystore & Android OAuth Client ID (CI/CD)**:
  - Generate release keystore locally:
    ```bash
    keytool -genkeypair -v -keystore release.keystore -alias healthcoach -keyalg RSA -keysize 2048 -validity 10000
    ```
  - Extract SHA-1: `keytool -list -v -keystore release.keystore -alias healthcoach`
  - Create a second Android OAuth Client ID in Google Cloud with package `com.lbthomas.healthcoach` and the release keystore SHA-1.
  - Store a secure offline backup of `release.keystore` and its passwords.

---

### Phase 2: Local Development Environment Setup

#### What's Happening
Local development continues using standard `./gradlew` commands. Credentials and SDK configurations are kept strictly in `local.properties` (ignored by Git) without requiring Docker or remote CI secrets.

#### Maintainer Action Items
- [ ] Configure `/local.properties` on your development machine:
  ```properties
  sdk.dir=/path/to/your/Android/Sdk
  google.clientId.desktop=YOUR_DESKTOP_CLIENT_ID.apps.googleusercontent.com
  google.clientSecret.desktop=YOUR_DESKTOP_CLIENT_SECRET
  google.clientId.android=YOUR_ANDROID_CLIENT_ID.apps.googleusercontent.com
  ```
- [ ] Verify local build and test suite:
  ```bash
  ./gradlew check
  ```

---

### Phase 3: Conventional Commits & AI Coding Agent Guidelines

#### What's Happening
To automate changelog generation and semantic version tagging, commit messages follow the Conventional Commits specification:
* `fix(<scope>): <description>` $\rightarrow$ Increments **PATCH** (`1.0.0` $\rightarrow$ `1.0.1`)
* `feat(<scope>): <description>` $\rightarrow$ Increments **MINOR** (`1.0.0` $\rightarrow$ `1.1.0`)
* `feat(<scope>)!:` or `BREAKING CHANGE:` $\rightarrow$ Increments **MAJOR** (`1.0.0` $\rightarrow$ `2.0.0`)
* `chore:`, `docs:`, `test:`, `refactor:`, `ci:` $\rightarrow$ Excluded from version bumps

#### Maintainer Action Items
- [ ] Adopt Conventional Commit formatting for personal Git commit messages (e.g. `feat(sync): add companion client discovery`).
- [ ] Configure IDE Git commit message templates or plugins if desired.

#### Implementation Deliverables
- [ ] Configure AI coding agent guidelines (`.junie/guidelines.md`) for Conventional Commit enforcement.
- [ ] Add Git tag SemVer derivation logic in `build.gradle.kts` and code-generate `AppBuildConfig.APP_VERSION`.

---

### Phase 4: GitHub Repository Settings & Secrets

#### What's Happening
GitHub Actions workflows require permissions to publish release tags, create release assets, and deploy to GitHub Pages. Private signing keys and API secrets are securely stored in GitHub Secrets, while non-sensitive IDs are kept in GitHub Variables.

#### Maintainer Action Items
- [ ] **Workflow Permissions**: On GitHub, navigate to **Settings** → **Actions** → **General** → set **Workflow permissions** to **Read and write permissions**.
- [ ] **Configure GitHub Secrets** (**Settings** → **Secrets and variables** → **Actions** → **Secrets**):
  - `RELEASE_KEYSTORE_BASE64`: Base64 string of `release.keystore` (`base64 -w 0 release.keystore`).
  - `RELEASE_KEYSTORE_PASSWORD`: Keystore password.
  - `RELEASE_KEY_ALIAS`: Key alias (`healthcoach`).
  - `RELEASE_KEY_PASSWORD`: Key password.
- [ ] **Configure GitHub Variables** (**Settings** → **Secrets and variables** → **Actions** → **Variables**):
  - `GOOGLE_CLIENT_ID_DESKTOP`: Desktop Client ID string.
  - `GOOGLE_CLIENT_SECRET_DESKTOP`: Desktop Client Secret string.
  - `GOOGLE_CLIENT_ID_ANDROID`: Android Client ID string.
- [ ] **Enable GitHub Pages**: In repository **Settings** → **Pages**, select **Deploy from a branch** (`main` / `/docs` folder).

---

### Phase 5: Automated CI/CD & Multi-Platform Packaging Matrix

#### What's Happening
Two primary workflows automate verification and release builds:
1. **PR Validation (`.github/workflows/pr-validation.yml`)**: Runs on pull requests to execute unit tests, Android lint, and SqlDelight migration verification (`verifyMigrations = true`) in a fork-safe manner without requiring access to repository secrets.
2. **Release Packaging (`.github/workflows/release.yml`)**: Triggered by release tags (`v*`) or manual dispatch. Builds all native binaries in parallel across Windows and Ubuntu runners, generates categorized changelogs from Conventional Commits, and publishes the GitHub Release.

#### Maintainer Action Items
- [ ] Trigger the initial release workflow via manual dispatch or by pushing a version tag (e.g., `v1.0.0`).
- [ ] Verify that generated binary artifacts (`HealthCoach.msi`, `HealthCoach.apk`, `HealthCoach.aab`, `HealthCoach.flatpak`, `HealthCoach.deb`, `HealthCoach.rpm`) attach cleanly to the GitHub Release.

---

### Phase 6: GitHub Pages Landing Page, Release History & In-App Companion Links

#### What's Happening
As soon as the initial release artifacts are published, the public GitHub Pages site is deployed at `https://bigtlb.github.io/healthcoach/`. It provides immediate binary downloads, hosts the full release history with commit summaries, displays the privacy policy and medical disclaimer, and serves as the destination for in-app companion client discovery links.

#### Maintainer Action Items
- [ ] Verify the live landing page at `https://bigtlb.github.io/healthcoach/`.
- [ ] Review the published Privacy Policy (`docs/privacy-policy.html`) and Medical Disclaimer (`docs/terms.html`).

#### Implementation Deliverables
- [ ] Create static landing page (`docs/index.html`) with direct download buttons for latest MSI, APK, and Flatpak/DEB binaries.
- [ ] Create release history view (`docs/releases.html`) with Conventional Commit changelog categories.
- [ ] Add `AppInfo.WEBSITE_URL` and add website links in `AboutTab.kt` and `SyncTab.kt` (companion client discovery banner for peer-to-peer sync).

---

### Phase 7: App Store Registrations & Listings (Google Play & Flathub)

#### What's Happening
HealthCoach is prepared for distribution via official application catalogs: the Google Play Store for Android and Flathub for Linux.

#### Maintainer Action Items (Google Play Console)
- [ ] Register Google Play Developer account at [Google Play Console](https://play.google.com/console/signup) ($25 one-time fee) and complete verification.
- [ ] Create app listing (`HealthCoach`), set default language to `en-US`, Free app.
- [ ] Complete store listing assets (512x512 icon, 1024x500 feature graphic, screenshots, descriptions).
- [ ] Complete policies:
  - **Privacy Policy**: `https://bigtlb.github.io/healthcoach/privacy-policy.html`
  - **Data Safety Form**: Declare "No data collected / No data shared".
  - **Health App Declarations**: Declare health tracking (weight, blood pressure, nutrition).
- [ ] Upload initial signed `.aab` (from Phase 5 release) to **Internal Testing** track to activate Play App Signing.
- [ ] **CRITICAL**: Go to **Setup** → **App Integrity**, copy the **App signing key certificate SHA-1**, and create a 3rd Android OAuth Client ID in Google Cloud Console.
- [ ] Enable **Google Play Developer API** in Google Cloud, create a Service Account with Release Manager permissions, download JSON key, and add to GitHub Secrets as `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
- [ ] Add Google Play badge to `docs/index.html` and version shield to `README.md`.

#### Maintainer Action Items (Flathub)
- [ ] Author AppStream metadata (`com.lbthomas.healthcoach.metainfo.xml`), `.desktop` file, and 512x512 icon.
- [ ] Fork `flathub/flathub`, create branch `com.lbthomas.healthcoach`, add Flatpak manifest, and submit PR.
- [ ] Once approved, add `FLATHUB_DEPLOY_KEY` to GitHub Secrets.
- [ ] Add Flathub badge to `docs/index.html` and version shield to `README.md`.

---

### Phase 8: Repeatable Store Publishing Workflows

#### What's Happening
Publishing updates to Google Play and Flathub is separated into independent, repeatable manual workflows (`workflow_dispatch`). This allows the maintainer to review release notes, choose target rollout percentages, or select specific test tracks before deploying.

#### Workflows Overview
1. **Google Play Deployment (`.github/workflows/deploy-play-store.yml`)**:
   - Maintainer selects `track` (`internal`, `alpha`, `beta`, `production`), `user_fraction` (e.g. `0.1` for 10%), and inputs "What's New" text.
   - Builds signed `.aab` and uploads via Google Play API using `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`.
2. **Flathub Deployment (`.github/workflows/deploy-flathub.yml`)**:
   - Maintainer selects release tag (e.g., `v1.1.0`) and enters changelog notes.
   - Computes SHA-256 hash of Linux binary, updates the Flathub repository manifest, and submits the update.

---

## Maintainer Action Items Checklist

Use this quick checklist to track all manual setup tasks across platforms:

### 📋 Initial Setup & Credentials
- [x] Approve sample Medical & Health Disclaimer text.
- [ ] Create Google Cloud project `HealthCoach Sync` and enable Google Drive API.
- [ ] Configure OAuth Consent screen (`drive.appdata` scope, add personal test user).
- [ ] Create Desktop OAuth Client ID in Google Cloud.
- [ ] Create Android OAuth Client ID for local debug keystore SHA-1.
- [ ] Generate production `release.keystore` and create Android OAuth Client ID for release SHA-1.
- [ ] Fill in `/local.properties` for local builds.
- [ ] Enable GitHub Actions read/write permissions in repository settings.
- [ ] Add GitHub Secrets (`RELEASE_KEYSTORE_BASE64`, passwords) and Variables (`GOOGLE_CLIENT_ID_*`).
- [ ] Enable GitHub Pages in repository settings.

### 📋 App Store Onboarding (Post Initial Release)
- [ ] Register Google Play Developer account ($25 fee) and fill in store listing details.
- [ ] Upload initial `.aab` to Google Play Console and copy **App signing key certificate SHA-1** into Google Cloud Console.
- [ ] Create Google Play API Service Account and add `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` secret to GitHub.
- [ ] Submit Flathub onboarding PR to `flathub/flathub` and add `FLATHUB_DEPLOY_KEY` secret.
- [ ] Add store badges to `docs/index.html` and store shields to `README.md`.
