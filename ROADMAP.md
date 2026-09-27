# HealthCoach Product Roadmap

This document outlines the planned feature roadmap and strategic milestones for HealthCoach. Each initiative links to a detailed specification document in the [`docs/roadmap/`](docs/roadmap/) directory.

---

## 🗺️ Roadmap Items

### 1. [Remote Database Synchronization](docs/roadmap/remote-sync.md)
* **Goal**: Enable seamless, private, cross-device database synchronization across Desktop (JVM) and Android without central server dependencies.
* **Storage Providers**:
  * **Local Folders & Mounted Drives** (*Implemented - Desktop & Android*): Directories synced by Proton Drive, Microsoft OneDrive, Dropbox, or local network mounts.
  * **Google Drive (`appDataFolder`)** (*Implemented on Desktop; Android in progress*): Secure, hidden sandbox partition utilizing PKCE OAuth 2.0.
  * **Peer-to-Peer LAN Client-Server** (*Active Plan / In Progress*): Direct Wi-Fi synchronization with symmetric Desktop/Android server & client, mDNS zero-config discovery, raw binary SQLite streaming, PIN pairing, live UI/DB reset, and access toast notifications.
  * **Future Targets**: Microsoft OneDrive, direct SMB2/SMB3 network shares.
* **Architecture**: 3-way snapshot differential merge (`BASE`, `LOCAL`, `REMOTE`), UUID primary keys, SQLite `PRAGMA user_version` schema compatibility gating, and optimistic concurrency control.
* **UI Features**: Dedicated Sync configuration pane in Settings with audit metadata, top bar animated sync action button with rotation and failure badge overlays, cancel confirmation modals, and toast notifications.
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
