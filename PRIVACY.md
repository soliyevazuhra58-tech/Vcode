# Privacy Policy for VCode

**Effective Date:** November 21, 2026

VCode is an offline-first native Android IDE. Your privacy and control over your source code are central to how the application is designed.

## 1. Data Collection & Telemetry
- **No Analytics or Tracking:** VCode does **not** collect usage analytics, crash telemetry, advertising identifiers, or behavioral tracking data.
- **No Remote Code Uploads:** VCode does **not** upload, transmit, or share your project files, editor buffers, terminal commands, or Git commits with any external server controlled by VCode.

## 2. Local Storage & Persistence
All projects, open tabs, editor preferences, and recent project metadata remain strictly on your device inside:
- App-private internal workspace storage (`Context.filesDir`)
- Local Room SQLite database (`vcode_ide.db`) for project and recent tab history
- Jetpack DataStore Preferences for theme, typography, tab size, auto-save, and layout settings

Uninstalling VCode removes app-private data unless you explicitly exported files or folders to shared storage.

## 3. Device Permissions & Android Storage Access Framework (SAF)
- **Storage Access Framework (`OpenDocument`, `OpenDocumentTree`, `CreateDocument`):** VCode uses Android's system file picker so you explicitly grant access only to the individual files or folders you choose to import or export.
- **File Sharing (`FileProvider`):** Used solely when you trigger a ZIP project export or file share action.
- **Network Access (`INTERNET`):** Used only when you explicitly preview web content that loads external assets inside the local `WebView`, or when you initiate a Git remote operation in Source Control.

## 4. Integrated Terminal & Live Preview
- **Terminal:** Commands run locally inside an isolated Android shell process (`/system/bin/sh`) and an embedded JavaScript runtime (Mozilla Rhino) on your device.
- **Live Preview:** Local web projects are served purely in-memory/locally via `WebViewAssetLoader` / `shouldInterceptRequest` (`https://vcode.local/`) without exposing an open network server.

## 5. Third-Party Open Source Components
VCode integrates open-source libraries that execute locally on your device, including Jetpack Compose, Room, DataStore, Sora Editor (`io.github.rosemoe:editor`), Eclipse JGit, and Mozilla Rhino.

## 6. Contact & License
For questions or source inspection, review the project repository and [License](LICENSE).
