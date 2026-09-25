# HealthCoach Product Roadmap

This document outlines the planned feature roadmap and strategic milestones for HealthCoach. Each initiative links to a detailed specification document in the [`docs/roadmap/`](docs/roadmap/) directory.

---

## 🗺️ Roadmap Items

### 1. [Remote Database Synchronization](docs/roadmap/remote-sync.md)
* **Goal**: Enable seamless, private, cross-device database synchronization across Desktop (JVM) and Android.
* **Storage Providers**: Local Folders / Mounted Drives (OneDrive, Dropbox, Proton Drive Desktop), SMB Network Shares (`smbj`), and Proton Drive (SDK / sync folder).
* **Architecture**: 3-way snapshot merge reconciliation (`BASE`, `LOCAL`, `REMOTE`), UUID primary keys, SQLite `user_version` schema compatibility gating, and optimistic concurrency control.
* **UI Features**: Dedicated Sync configuration pane in Settings with audit metadata, top bar sync action button with rotation animation, cancel confirmation, error badge overlays, and toast notifications.
* **Detailed Spec**: [`docs/roadmap/remote-sync.md`](docs/roadmap/remote-sync.md)

---

### 2. [Food Journaling (Phase 1)](docs/roadmap/food-journaling.md)
* **Goal**: Expand health tracking to dietary habits and daily caloric intake.
* **Phase 1 Scope**: Daily food and calorie logging, meal categorization (Breakfast, Lunch, Dinner, Snacks), search, and fast reuse of past entries.
* **Longer-Tail Roadmap**:
  * Standard nutritional database imports (e.g., USDA FoodData Central).
  * Mobile barcode scanning for packaged goods.
  * Searchable community nutrition databases and data contribution.
* **Detailed Spec**: [`docs/roadmap/food-journaling.md`](docs/roadmap/food-journaling.md)

---

*Note: Roadmap priorities and technical specifications are subject to refinement based on implementation feedback and user needs.*
