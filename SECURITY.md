# Security Policy

HealthCoach is dedicated to protecting user data sovereignty and personal health privacy. Because this application processes and stores sensitive personal wellness data, rapid identification, responsible reporting, and timely remediation of security vulnerabilities are essential to minimize harm and maximize user safety.

---

## Supported Versions

Security updates and critical patches are prioritized for active releases:

| Version | Supported | Notes |
| :--- | :--- | :--- |
| `0.10.x` (or current `main`) | :white_check_mark: | Actively maintained release track |
| `< 0.10.0` | :x: | Upgrade to the latest release for security updates |

Users are strongly encouraged to stay on the latest available release to ensure all dependency updates and security patches are in place.

---

## Reporting a Vulnerability

**Please do not report security vulnerabilities through public GitHub issues, discussions, or pull requests.**

Public disclosure prior to remediation increases the risk of exploitation. Because GitHub Private Vulnerability Reporting is enabled for this repository, please report vulnerabilities privately through GitHub:

1. Navigate to the **Security** tab of the [HealthCoach repository](https://github.com/bigtlb/healthcoach/security).
2. Click **Report a vulnerability** under "Private vulnerability reporting".
3. Fill out the advisory form with details about the issue and submit it.

### Information to Include
To help evaluate, triage, and resolve the vulnerability efficiently, please provide:
- A clear description of the vulnerability and its potential impact.
- Affected platforms (Android, Linux Desktop / Flatpak / DEB, Windows, macOS).
- Step-by-step reproduction instructions or a minimal Proof of Concept (PoC).
- Any proposed remediation, patch, or mitigation, if available.

---

## Response & Remediation Process

To protect users and resolve vulnerabilities with maximum efficacy, the following response timeline is observed:

1. **Initial Acknowledgment**: Within **48 hours** of report submission.
2. **Triage & Impact Assessment**: Confirmation of validity and severity classification within **5 business days**.
3. **Patch & Coordinated Disclosure**:
   - A fix is developed in a private security fork/branch.
   - An incremental release containing the security patch is published.
   - A public GitHub Security Advisory is published detailing the remediation and crediting the reporter (unless anonymity is requested).

---

## Scope & Security Guarantees

When evaluating potential vulnerabilities, consider HealthCoach's core architecture and trust model:

- **Strict Local-First Data Sovereignty**: All health metrics reside in the local SQLite database via SqlDelight. Flaws allowing unauthorized local process data extraction, SQL injection, or unprompted data exfiltration are treated with the highest priority.
- **Peer-to-Peer (LAN) Synchronization**: The embedded Ktor P2P server and mDNS discovery mechanism must enforce pairing token authentication and restrict access to authorized local peers.
- **Zero Telemetry Guarantee**: HealthCoach does not contain tracking beacons, analytics SDKs, or unprompted remote network calls. Any unauthorized remote background data transmission is treated as a critical defect.
- **Cloud Sync Sandboxing**: Google Drive synchronization must strictly confine file operations to the sandboxed `appDataFolder` and protect OAuth refresh tokens using platform-appropriate secure storage.

---

## Safe Harbor

Good-faith security research is a vital practice that enhances overall system reliability and user protection. Maintainers will not pursue legal action against researchers who:
- Act in good faith to avoid privacy violations, data destruction, and service interruption.
- Maintain confidentiality and allow maintainers reasonable time to remediate the vulnerability before public disclosure.
- Comply with applicable laws and refrain from accessing or modifying user data without authorization.
