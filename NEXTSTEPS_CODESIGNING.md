# HealthCoach Code Signing Guide: SignPath Application & Setup

## 1. Executive Summary

Unsigned Windows binaries (`.msi` / `.exe`) trigger Microsoft Defender SmartScreen warnings ("Unknown Publisher", "Windows protected your PC") on Windows 11. To eliminate these warnings and establish verified publisher trust, HealthCoach releases must be digitally signed with an Authenticode certificate.

**Why SignPath for Open Source:**
- **Free for Open Source**: SignPath Foundation provides free code signing certificates and automated cloud signing services to qualifying open-source projects.
- **Zero Hardware Requirement**: Complies with CA/Browser Forum hardware security requirements via a managed Cloud HSM without needing physical USB dongles or dedicated local signing servers.
- **Native GitHub Actions Integration**: Integrates directly into CI/CD release workflows via official SignPath GitHub Actions.

---

## 2. User Action Items

Follow these sequential steps to apply for SignPath, obtain approval, and enable automated release signing.

### Step 1: Submit Open Source Application to SignPath

1. Navigate to the [SignPath Open Source Program Application](https://signpath.org/apply.html) (or explore [SignPath Foundation](https://signpath.org)).
2. Complete the application form with HealthCoach project details:
   - **Project Name**: HealthCoach
   - **Repository URL**: `https://github.com/bigtlb/healthcoach`
   - **Project Website / Domain**: `https://lbthomas.com` (or your GitHub repo URL)
   - **Open Source License**: MIT / Apache-2.0
   - **Project Description**: Multiplatform personal health, nutrition, and wellness tracking application built with Compose Multiplatform.
   - **Contact / Maintainer**: Your name and email address.
3. Submit the form and wait for review and approval from the SignPath Foundation team (typically 1–3 business days).

---

### Step 2: Configure SignPath Project & Signing Policy

Once your application is approved and you receive access to your SignPath organization dashboard:

1. **Create Project**:
   - Create a project named `HealthCoach`.
   - Note the **Project Slug** (e.g., `healthcoach`).
2. **Define Signing Policy**:
   - Navigate to **Signing Policies** and create a policy for release builds (e.g., `test-signing` for staging or `release-signing` for official releases).
   - Note the **Signing Policy Slug** (e.g., `release-signing`).
3. **Configure Build & Artifact Settings**:
   - Target Artifact: Windows Installer (`*.msi`) and executables (`*.exe`).
   - Signing Service: Authenticode SHA-256 with RFC 3161 timestamping.
4. **Generate API Token**:
   - Go to **Settings** $\rightarrow$ **API Tokens** $\rightarrow$ **Generate New Token**.
   - Copy the generated API token securely.

---

### Step 3: Configure GitHub Repository Secrets & Variables

In your GitHub repository, go to **Settings** $\rightarrow$ **Secrets and variables** $\rightarrow$ **Actions** and add the following:

#### Secrets:
* **`SIGNPATH_API_TOKEN`**: The API token generated in Step 2.

#### Variables (Optional / Configurable):
* **`SIGNPATH_ORGANIZATION_ID`**: Your SignPath organization identifier.
* **`SIGNPATH_PROJECT_SLUG`**: `healthcoach`
* **`SIGNPATH_SIGNING_POLICY_SLUG`**: `release-signing`

---

### Step 4: GitHub Actions CI/CD Integration

In `.github/workflows/release.yml`, update the Windows release job to submit the built `.msi` package to SignPath for signing:

```yaml
      - name: Sign MSI with SignPath
        if: env.SIGNPATH_API_TOKEN != ''
        uses: signpath/github-action-submit-signing-request@v1.1
        with:
          api-token: ${{ secrets.SIGNPATH_API_TOKEN }}
          organization-id: ${{ vars.SIGNPATH_ORGANIZATION_ID }}
          project-slug: ${{ vars.SIGNPATH_PROJECT_SLUG || 'healthcoach' }}
          signing-policy-slug: ${{ vars.SIGNPATH_SIGNING_POLICY_SLUG || 'release-signing' }}
          github-token: ${{ secrets.GITHUB_TOKEN }}
          wait-for-completion: true
          output-artifact-directory: desktopApp/build/compose/binaries/main-release/msi
          parameters: |
            files:
              - "desktopApp/build/compose/binaries/main-release/msi/*.msi"
```

---

### Step 5: Verification Checklist

- [ ] **Application Approved**: Confirmation received from SignPath Foundation.
- [ ] **Secrets Stored**: `SIGNPATH_API_TOKEN` configured in GitHub Repository Secrets.
- [ ] **Test Signing Run**: Trigger `.github/workflows/release.yml` via `workflow_dispatch` on a test tag.
- [ ] **Signature Verification**: Inspect generated `.msi` on Windows:
  - Right-click `.msi` $\rightarrow$ **Properties** $\rightarrow$ **Digital Signatures**.
  - Verify publisher shows the SignPath-certified open-source identity and signature digest is SHA-256 with timestamp.
- [ ] **SmartScreen Check**: Run the installer in Windows Sandbox or Windows 11 to confirm the verified publisher name displays cleanly.
