# HealthCoach Product Roadmap

This document outlines the development status, active roadmap priorities, and strategic future initiatives for HealthCoach.

---

## 🚀 Status Overview

HealthCoach's core functional architecture—spanning health metric tracking, clinical classifications, dietary logging, metabolic goal planning, multi-axis visualization, and cross-device synchronization—is **nearly complete**. Current development focus is concentrated on finalizing mobile sync registration and establishing production CI/CD packaging pipelines.

---

## ✅ Completed Milestones

The following capabilities have been designed, implemented, and verified in the codebase:

* **⚖️ Weight Tracking & History**:
  * Imperial (`lbs`) and Metric (`kg`) unit management with instant conversion.
  * Chronological grouping by Month/Year with Day-of-Week badges.
  * Consecutive weigh-in delta indicators and monthly net change summaries.
  * In-line editing, updating, and confirmed deletion workflows.

* **🩺 Blood Pressure & Pulse Management**:
  * Tri-metric capture of Systolic, Diastolic, and Pulse readings.
  * Live clinical classification aligned with official American Heart Association (AHA) guidelines.
  * Interactive criteria tooltips and direct AHA guideline references.

* **🥗 Food Journaling & Caloric Tracking (Phase 1)**:
  * Daily meal organization across Breakfast, Lunch, Dinner, and Snacks.
  * Master food library with standardized serving units and caloric densities.
  * Fast re-logging from Recent and Frequent food items.
  * Dynamic portion calculator with real-time calorie recalculation.
  * Daily summary card tracking consumed calories against maintenance and goal targets.
  * *Detailed Spec*: [`docs/roadmap/food-journaling.md`](docs/roadmap/food-journaling.md)

* **👤 User Profile & Metabolic Goals**:
  * Mifflin-St Jeor BMR and TDEE basal metabolic rate calculations.
  * Flexible goal modes: Rate-based deficit/surplus, Timeline-based targets, or direct manual calorie overrides.
  * Visual goal projection timelines comparing projected vs. actual trajectories.

* **📈 Compound Multi-Axis Graphs**:
  * Multi-layer time-series visualization powered by Vico with independent Y-axis scaling for Weight, Blood Pressure/Pulse, and Calories.
  * Quantized graduation stepping strictly divisible by 5 (for Weight and BP) and 50 (for Calories), dynamically adapting step sizes (5, 10, 15, 20, 25, 30, 50) to prevent visual crowding.
  * Long-timeframe date formatting condensing spans exceeding 2 years into `YY/MM` (e.g., `26/01`).
  * Desktop hover and mobile touch/drag marker tooltips.
  * Dynamic series toggles for Weight, Systolic, Diastolic, Pulse, Calories, and Metabolic Reference Lines.

* **🔄 Cross-Device Synchronization Engine**:
  * Robust 3-way differential merge algorithm (`BASE`, `LOCAL`, `REMOTE`) with UUID primary keys and `updated_at` last-write-wins conflict resolution.
  * Pre-sync SQLite `PRAGMA user_version` schema compatibility checks.
  * **Local Folder Adapter**: Direct sync with local directories, USB drives, or cloud-mounted folders (Proton Drive, OneDrive, Dropbox).
  * **Desktop Google Drive Adapter**: RFC 7636 PKCE OAuth 2.0 desktop synchronization directly with Google Drive's private `appDataFolder`.
  * **Peer-to-Peer LAN Synchronization**: Symmetric embedded Ktor CIO server and client for direct Wi-Fi sync between Desktop and Android devices with mDNS zero-configuration discovery and 6-digit PIN authentication.
  * Modernized Sync UI with animated status button, error badge overlays, audit metadata, and connection toast notifications.
  * *Detailed Spec*: [`docs/roadmap/remote-sync.md`](docs/roadmap/remote-sync.md)

* **🎨 Responsive UI & Material 3 Theming**:
  * Adaptive dual-pane split view with adjustable splitter on wide desktop screens.
  * Responsive single-pane navigation flow on mobile devices.
  * 4 curated Material 3 themes (Default, Blue, Green, Teal) in Light, Dark, and System modes.

---

## 📌 Active Roadmap Items (Remaining Implementation)

### 1. 🔄 Mobile Google Drive Sync Registration
* **Objective**: Finalize the Android OAuth 2.0 client registration and authorization flow for Google Drive's `appDataFolder` sandbox partition.
* **Key Deliverables**:
  * Complete Android-side OAuth token retrieval and credential persistence.
  * Verify SHA-1 keystore binding and credential resolution between Android and Google Cloud Console.
  * End-to-end sync testing between Android and Desktop via the shared Google Drive `appDataFolder`.
* **Setup & Architecture Guide**: See [`NEXTSTEPS.md`](NEXTSTEPS.md) for detailed configuration steps and multi-workstation keystore management.

### 2. 📦 Automated CI/CD Multiplatform Packaging Pipeline
* **Objective**: Establish automated GitHub Actions workflows to build, sign, package, and release production-ready distribution artifacts across all supported platforms.
* **Target Packaging Artifacts**:
  * **Android App**: Automated release APK and signed Android App Bundle (`.aab`) builds with keystore secrets.
  * **Linux App**: Native Linux **Flatpak** (`.flatpak`) packaging and desktop environment metadata integration.
  * **Windows App**: Native Windows installer packages (`.msi` / `.exe`) with embedded JVM runtime.

---

## 🔭 Future Work & Strategic Exploration

### 1. 🍏 Apple Ecosystem Expansion (macOS & iPhone)
* **macOS Desktop Package**: Native macOS application bundles (`.dmg` / `.pkg`) with code signing and Apple Notarization support.
* **iPhone / iOS Target**: Explore porting the shared Kotlin Multiplatform UI to iOS using Compose Multiplatform for iOS, enabling complete cross-platform parity across Android, iOS, Windows, Linux, and macOS.

### 2. 🥗 Food Journaling Phase 2 & Advanced Nutrition
* **Standard Food Database Integration**: Offline and online querying of standard nutritional datasets (e.g., USDA FoodData Central) for comprehensive macronutrient profiles (protein, carbs, fats, fiber).
* **Mobile Barcode Scanner**: Camera-based UPC/EAN barcode scanning on mobile devices for instant packaged goods logging (e.g., Open Food Facts).
* **Crowdsourced Nutrition Sharing**: User-contributed nutritional item verification and community sharing.

### 3. ☁️ Additional Cloud Storage Adapters
* **Microsoft OneDrive Adapter**: Direct Microsoft Graph API integration using personal OneDrive application storage.
* **Direct WebDAV / SMB Network Shares**: Native network share storage adapter for NAS devices and self-hosted storage.

### 4. 📊 Multi-Format Data Export & Reporting Engine
* **Overview**: Implement comprehensive data export and archival capabilities (accessible via Settings / About) to provide complete data portability, external analysis, and clinical reporting for healthcare providers.
* **Supported Export Formats**:
  * **Microsoft Excel (`.xlsx`)**: Single structured workbook containing dedicated worksheets/tabs for each exported dataset.
  * **OpenDocument Spreadsheet (`.ods`)**: Multi-tab open spreadsheet format with dedicated sheets for each dataset.
  * **CSV Archive (`.zip`)**: Compressed archive containing individual, cleanly formatted CSV files for each selected metric dataset.
  * **PDF Clinical Report (`.pdf`)**: Formatted summary documents with tabular logs and metric visualizations designed for printing or sharing with physicians.
* **Granular Export Customization**:
  * **Flexible Timeframe Filtering**: Export data by custom date ranges or standard intervals (e.g., All Time, Past 30 Days, Past 90 Days, Year-to-Date).
  * **Metric & Dataset Selection**: Selective checkboxes allowing users to customize which data series to include (Weight logs, Blood Pressure & Pulse records, Daily Calorie summaries, and detailed Food Journal entries).

---

*Note: For implementation details, setup instructions, and credential configuration guides, refer to [`NEXTSTEPS.md`](NEXTSTEPS.md) and [`docs/roadmap/`](docs/roadmap/).*
