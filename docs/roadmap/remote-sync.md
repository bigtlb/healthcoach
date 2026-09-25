# Remote Database Synchronization

### Status: In Design & Planning

---

## 1. Overview & Objectives

HealthCoach is a local-first application where health metrics (body weights, blood pressure readings, pulse) are stored in an embedded SQLite database. The goal of this initiative is to enable private, cross-device synchronization between Desktop (JVM) and Android devices without requiring a centralized, proprietary backend server.

Users will be able to synchronize their health records using their own storage targets:
* **Local Folders & Mounted Drives**: Directories synchronized by Proton Drive Desktop, Microsoft OneDrive, Dropbox, or local network mounts.
* **SMB Network Shares**: Direct SMB2/SMB3 integration via `smbj` for private NAS / home servers.
* **Proton Drive**: Proton Drive SDK integration and local desktop sync folder integration.

---

## 2. Core Architecture: 3-Way Snapshot Merge

Direct file copying over network shares risks corruption and completely overwrites concurrent modifications made on other devices. HealthCoach employs a cached 3-way snapshot merge reconciliation algorithm:

```
┌─────────────────────────────────────────────────────────┐
│                    3-Way Merge Engine                   │
├───────────────────┬─────────────────────────────────────┤
│ BASE              │ Cached snapshot from last sync      │
│ (synced_cache.db) │ (reference point for diffs)         │
├───────────────────┼─────────────────────────────────────┤
│ LOCAL             │ Live active user database           │
│ (live.db)         │ (contains recent local edits)       │
├───────────────────┼─────────────────────────────────────┤
│ REMOTE            │ Snapshot downloaded from storage    │
│ (remote.db)       │ (contains remote device edits)      │
└───────────────────┴─────────────────────────────────────┘
```

### Reconciliation Workflow:
1. **Metadata Inspection & Concurrency Check**:
   * Inspect remote file metadata (SHA-256 hash, modified timestamp).
   * If remote file is missing (initial sync): Upload local database and initialize base cache.
   * If remote hash matches `last_synced_hash` and local database is unmodified: No-op.
2. **Pre-Sync Schema Compatibility Gates**:
   * SQLite `PRAGMA user_version` is checked on downloaded remote databases prior to merging.
   * If `local < remote`: Abort sync with alert prompting user to update the app.
   * If `local > remote`: Warn and confirm before migrating and upgrading remote schema.
3. **Differential Computation**:
   * Compute **Remote Differential** (`REMOTE` vs `BASE`): Detect remote inserts, updates (`updated_at > base.updated_at`), and deletions (present in `BASE` but absent in `REMOTE`).
   * Compute **Local Differential** (`LOCAL` vs `BASE`): Detect local inserts, updates, and deletions.
4. **Conflict Resolution (Last-Write-Wins)**:
   * Non-conflicting changes are applied bidirectionally to `LOCAL` and `REMOTE`.
   * For concurrent modifications on the same record, the row with the newer `updated_at` timestamp takes precedence.
   * Deletions vs modifications: Updates take precedence over deletions.
5. **Fresh Device Bootstrap & Initial Union Merge**:
   * If base cache is missing on an empty local install: All remote records are pulled down cleanly.
   * If base cache is missing on a populated local database: Perform a non-destructive union merge of all records.
6. **Optimistic Concurrency & Atomic Upload**:
   * Remote SHA-256 hash is validated before upload to ensure no concurrent modification occurred during merge computation; retries with fresh snapshot if conflicted.
   * Updated database is uploaded, `synced_cache.db` is updated, and sync metadata is recorded.

---

## 3. Storage Provider Abstraction (`RemoteStorageAdapter`)

An extensible provider interface decouples transport protocols from merge logic:

* Parameterized `remoteDirectoryPath` and `remoteFileName` (defaulting to `healthcoach.db`) to support custom profiles and environments.
* Methods: `getFileMetadata()`, `downloadFile()`, `uploadFile()`, and `testConnection()`.
* **Phase 1 Providers**:
  * `LocalFolderAdapter`: Local file paths, external drives, and desktop cloud client folders.
  * `SmbStorageAdapter`: Windows / Samba network shares via `smbj`.
  * `ProtonDriveStorageAdapter`: Proton Drive SDK and local sync client directory integration.

---

## 4. UI & User Experience

* **Top Bar Sync Action Button**:
  * Appears in `AppBar` next to Settings only when a sync provider is configured.
  * Continuously animates with a rotating icon during active sync.
  * Tapping while syncing displays a confirmation prompt to safely cancel the transfer.
  * Overlays a persistent red exclamation badge if the previous sync failed.
* **Toast & Snackbar Notifications**:
  * Dispatches non-intrusive status updates for sync initiation, success, conflicts, and failures.
* **Modernized Settings Pane**:
  * Categorized Master-Detail sidebar navigation.
  * Comprehensive Sync settings: Provider selection, credentials, storage path, custom DB filename, and connection test.
  * **Sync Audit & Diagnostics**: Displays last synced timestamp, remote snapshot hash, sync status, and active SQLite schema version (`user_version`).
  * **Background Sync Options**: Auto-sync on application close/exit and scheduled periodic intervals.

---

## 5. Delivery Phases

1. **Phase 1: Schema & Data Model Evolution**:
   * Migrate tables to UUID primary keys (`TEXT PRIMARY KEY`).
   * Add `updated_at` epoch timestamps to all tables.
   * Implement SQLite `PRAGMA user_version` helpers and SQLDelight migration scripts.
2. **Phase 2: Remote Storage Adapters**:
   * Implement `LocalFolderAdapter`, `SmbStorageAdapter`, and `ProtonDriveStorageAdapter`.
3. **Phase 3: 3-Way Snapshot Merge Engine**:
   * Implement snapshot cache management, schema gating, bootstrap union merge, and optimistic concurrency retry loops.
4. **Phase 4: Settings UI & Sync Configuration**:
   * Modernize Settings Dialog navigation, add Sync pane, diagnostics display, and auto-sync triggers.
5. **Phase 5: Top Bar Sync Action & Toast Feedback**:
   * Add animated `SyncActionButton`, failure overlays, cancel confirmation modals, and app exit lifecycle hooks.
