# Google Drive Sync Setup & Next Steps

This guide provides step-by-step instructions to configure **Google Drive (appDataFolder)** synchronization for HealthCoach on both **Desktop (JVM)** and **Android**, configure local development settings, and set up GitHub for continuous integration.

---

### Overview: How Google Drive Sync Works

HealthCoach synchronizes SQLite health records using Google Drive's dedicated **Application Data Folder (`appDataFolder`)**:
- **Private Sandbox**: The `appDataFolder` is a hidden, application-specific partition in the user's Google Drive accessible strictly through the scoped permission `https://www.googleapis.com/auth/drive.appdata`.
- **Zero User Interference**: Database files are completely hidden from regular Google Drive file listings, preventing accidental user deletion, renaming, or corruption.
- **Cross-Platform Sync**: When both your Desktop app and Android app authenticate against the same Google account, they access and synchronize the **exact same remote database file** (`healthcoach.db`).

---

### Step 1: Create a Google Cloud Project

1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Click the project dropdown at the top of the page and select **New Project**.
3. Set the **Project name** to `HealthCoach Sync` (or any name you prefer).
4. Click **Create** and ensure your new project is selected in the top bar.

---

### Step 2: Enable the Google Drive API

1. In the Google Cloud navigation menu (left sidebar), go to **APIs & Services** → **Library**.
2. Search for **Google Drive API**.
3. Select **Google Drive API** and click **Enable**.

---

### Step 3: Configure the OAuth Consent Screen

1. In the left menu, go to **APIs & Services** → **OAuth consent screen**.
2. Select **External** (allows any Google account to sign in for personal use) and click **Create**.
3. Fill in the **App information**:
   - **App name**: `HealthCoach`
   - **User support email**: Select your Google email address.
   - **Developer contact information**: Enter your email address.
   - Click **Save and Continue**.
4. **Scopes**:
   - Click **Add or Remove Scopes**.
   - In the filter box, enter `drive.appdata`.
   - Check the box for `.../auth/drive.appdata` (*See, create, and delete its own configuration data in your Google Drive*).
   - Click **Update** → **Save and Continue**.
5. **Test Users**:
   - Because the project is in "Testing" mode, only designated test users can log in (up to 100 accounts).
   - Click **Add Users** and enter your personal Google account email address.
   - Click **Save and Continue** → **Back to Dashboard**.

---

### Step 4: Create OAuth 2.0 Credentials

You will create **two** Client IDs under this same Google Cloud project: one for **Desktop** and one for **Android**.

#### 1. Create the Desktop Client ID (PKCE Flow)
1. Go to **APIs & Services** → **Credentials**.
2. Click **Create Credentials** → **OAuth client ID**.
3. For **Application type**, select **Desktop app**.
4. Set the **Name** to `HealthCoach Desktop`.
5. Click **Create**.
6. Copy the generated **Client ID** (it will look like `xxxxxxxxxxxx-xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx.apps.googleusercontent.com`).

#### 2. Create the Android Client ID (Cryptographic Keystore Binding)
1. In **Credentials**, click **Create Credentials** → **OAuth client ID**.
2. For **Application type**, select **Android**.
3. Set the **Name** to `HealthCoach Android (Dev Machine 1)`.
4. Enter the **Package name**: `com.lbthomas.healthcoach`
5. Obtain your local Android debug keystore **SHA-1 fingerprint**:
   - Run the following command in your terminal:
     ```bash
     keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
     ```
   - *Alternatively, run Gradle's signing report:*
     ```bash
     ./gradlew :androidApp:signingReport
     ```
   - Copy the **SHA1** fingerprint line (e.g., `AA:BB:CC:DD:EE:...`).
6. Paste the SHA-1 fingerprint into the **SHA-1 certificate fingerprint** field in Google Cloud Console.
7. Click **Create** and copy the generated **Android Client ID**.

##### Managing Multiple Dev Machines & CI/CD Pipelines
Google Play Services verifies Android OAuth 2.0 requests at runtime using the tuple `(Package Name, Signing Keystore SHA-1)`. Because local workstations generate an independent `~/.android/debug.keystore` upon SDK installation, builds produced on different machines or CI runners have distinct SHA-1 fingerprints.

Below are the practical workflows to maximize developer velocity, ease of maintenance, and pipeline reliability across environments:

---

#### Strategy 1: Multiple Android Client IDs in Google Cloud (Current Active Approach)
*Best for rapid setup without modifying project files or sharing local keystores.*

1. **Keep Independent Local Keystores**: Continue building with the default local debug keystore on each machine.
2. **Obtain Workstation SHA-1**:
   - On Workstation A (e.g. Desktop): Run `./gradlew :androidApp:signingReport` or `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`.
   - On Workstation B (e.g. Laptop): Run `./gradlew :androidApp:signingReport`.
3. **Register Each Workstation in Google Cloud Console**:
   - Go to **APIs & Services** → **Credentials** → **Create Credentials** → **OAuth client ID** → **Android**.
   - Set **Package name** to `com.lbthomas.healthcoach`.
   - Client ID 1: Name `HealthCoach Android - Desktop Workstation`, paste Desktop SHA-1.
   - Client ID 2: Name `HealthCoach Android - Laptop`, paste Laptop SHA-1.
4. **Runtime Resolution**: Google Play Services automatically verifies incoming requests against any registered SHA-1 under the project matching `com.lbthomas.healthcoach`. You can configure either Client ID in `local.properties`.

---

#### Strategy 2: Shared Repository Debug Keystore (Future Option for Unified Builds)
*Best for eliminating multi-client management so all machines and CI debug runs share identical signatures.*

1. **Commit a Shared Debug Keystore**:
   Copy a debug keystore into `androidApp/debug.keystore` (or generate one):
   ```bash
   keytool -genkeypair -v -keystore androidApp/debug.keystore -alias androiddebugkey -keypass android -storepass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
   ```
2. **Configure `androidApp/build.gradle.kts`**:
   ```kotlin
   android {
       signingConfigs {
           getByName("debug") {
               storeFile = file("debug.keystore")
               storePassword = "android"
               keyAlias = "androiddebugkey"
               keyPassword = "android"
           }
       }
   }
   ```
3. **Single Google Cloud Client ID**: Only one debug Client ID is needed in Google Cloud Console matching the shared keystore's SHA-1.

---

#### Strategy 3: CI/CD Release Builds & Google Play Distribution
*Required for reproducible production builds in GitHub Actions and Play Store distribution.*

1. **Generate Production Keystore**:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias healthcoach -keyalg RSA -keysize 2048 -validity 10000
   ```
2. **Obtain SHA-1 Fingerprint**:
   ```bash
   keytool -list -v -keystore release.keystore -alias healthcoach
   ```
3. **Register Release Client ID in Google Cloud Console**:
   - Create an Android Client ID named `HealthCoach Android - Release` with package `com.lbthomas.healthcoach` and the release SHA-1.
4. **Configure GitHub Secrets**:
   - Store base64-encoded keystore: `base64 -w 0 release.keystore` → Secret: `KEYSTORE_BASE64`
   - Store passwords: `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
5. **Google Play App Signing (if distributed via Play Store)**:
   - Google Play re-signs release APKs with Google's managed key. Copy the **App signing key certificate SHA-1** from Google Play Console (**Setup** → **App Integrity**) and register it as an additional Android Client ID in Google Cloud Console.

---

### Step 5: Configure `local.properties` for Local Development

Open `local.properties` in the project root and replace the dummy placeholder values with your real Google Client IDs:

```properties
sdk.dir=/home/thomas/Android/Sdk

# Google Drive Sync Configuration (OAuth 2.0 Client Credentials)
# Desktop Client ID & Secret (from Google Cloud Console -> Credentials -> Desktop app)
google.clientId.desktop=YOUR_ACTUAL_DESKTOP_CLIENT_ID.apps.googleusercontent.com
google.clientSecret.desktop=YOUR_ACTUAL_DESKTOP_CLIENT_SECRET

# Android Client ID (from Google Cloud Console -> Credentials -> Android)
google.clientId.android=YOUR_ACTUAL_ANDROID_CLIENT_ID.apps.googleusercontent.com
```

> **Note**: `local.properties` is included in `.gitignore` and will never be committed to Git. During the Gradle build (`:shared:generateGoogleAuthConfig`), Gradle automatically reads these properties (or environment variables) and generates `GoogleAuthConfig.kt` into the `build/` directory, embedding the client credentials directly into the compiled application binaries so neither `local.properties` nor environment variables are needed by end-users at runtime.

---

### Step 6: Configure GitHub Secrets / Variables for CI/CD

If you use GitHub Actions to build and release distributed binaries:

1. Open your repository on GitHub.
2. Navigate to **Settings** → **Secrets and variables** → **Actions**.
3. Under the **Variables** (or **Secrets**) tab, add:
   - `GOOGLE_CLIENT_ID_DESKTOP`: Value of your Desktop OAuth Client ID.
   - `GOOGLE_CLIENT_ID_ANDROID`: Value of your Android OAuth Client ID.
4. **Why this protects your repository**:
   - GitHub Actions secrets/variables are **not accessible by forks**.
   - If someone forks your repository, their automated CI builds cannot consume your Google Cloud API quotas. Fork maintainers can configure their own free Google Cloud projects.

---

### Step 7: Testing & Verification

#### 1. Running the Desktop Application
1. Launch the desktop app:
   ```bash
   ./gradlew :desktopApp:run
   ```
2. Open **Settings** (gear icon) → **Sync** → **Storage & Auth**.
3. Select **Google Drive (appDataFolder)**.
4. Click **Validate / Connect**.
5. Add or edit a weight or blood pressure entry, then click the **Sync** button in the top bar.
6. Verify the success notification: *"Sync successful: X uploaded, Y downloaded"*.

#### 2. Running the Android Application
1. Install and run the Android app on your device or emulator:
   ```bash
   ./gradlew :androidApp:installDebug
   ```
2. Open **Settings** → **Sync** and enable Google Drive synchronization.
3. Trigger a sync and verify that entries created on Desktop appear automatically on Android.

#### 3. Inspecting Storage in Google Drive
1. Open [Google Drive on the Web](https://drive.google.com).
2. Click **Settings** (gear icon) → **Settings** → **Manage Apps**.
3. Locate **HealthCoach** in the list.
4. Verify the entry displays **"Hidden app data: <size> KB"**, confirming that the database is isolated and protected.
