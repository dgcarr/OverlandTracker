# OverlandTracker

## Run this app on an Android emulator (Windows, step by step)

Follow these steps in order.

### 1) Install required tools

1. Install **Android Studio** (includes Android SDK, emulator, and platform tools):
   - https://developer.android.com/studio
2. During setup, keep default components enabled (especially **Android SDK**, **Android SDK Platform-Tools**, and **Android Virtual Device**).
3. Install **Git for Windows** (if not already installed):
   - https://git-scm.com/download/win
4. Install **Gradle 8.7+** on Windows (required because this repo does not commit `gradle-wrapper.jar`):
   - https://gradle.org/install/

> Tip: After installing Gradle, open a new terminal and run `gradle -v` to verify it works.

### 2) Get the source code

Use either Android Studio (Git clone) or command line.

```powershell
git clone <YOUR_REPOSITORY_URL>
cd OverlandTracker
```

### 3) Regenerate the Gradle wrapper JAR (required for this repo)

From the project root:

```powershell
gradle wrapper --gradle-version 8.7 --no-validate-url
```

This creates `gradle/wrapper/gradle-wrapper.jar` locally so `gradlew` commands can run.

If your Gradle command is named differently (for example `gradle8`), use that command instead.

### 4) Open the project in Android Studio

1. Start Android Studio.
2. Click **Open**.
3. Select the `OverlandTracker` folder.
4. Wait for Gradle sync to finish.

### 5) Create an Android emulator

1. In Android Studio, open **Device Manager**.
2. Click **Create device**.
3. Choose a phone definition (for example, Pixel 7) and click **Next**.
4. Select a recent Android system image (for example API 34) and download it if prompted.
5. Finish creation and start the emulator.

### 6) Run the app on the emulator

1. In Android Studio, set the run target to your emulator.
2. Click **Run** (green play button) for the `app` configuration.
3. Wait for build + install; the app should launch automatically.

---

## Quick command-line run option (after setup)

If the emulator is already running, you can build/install from terminal:

```powershell
.\gradlew.bat installDebug
```

If needed, start the app from Android Studio after install.

---

## Troubleshooting (Windows)

- **`gradlew` fails because wrapper JAR is missing**
  - Re-run:
    ```powershell
    gradle wrapper --gradle-version 8.7 --no-validate-url
    ```

- **Android Studio says SDK tools are missing**
  - Open **SDK Manager** and install/update SDK Platform + Platform-Tools + Build-Tools.

- **Emulator is very slow**
  - Enable hardware virtualization in BIOS/UEFI.
  - Enable Windows Hypervisor Platform (if required by your emulator configuration).

- **No devices found when running app**
  - Confirm emulator is started and visible in Device Manager.
  - Re-run with:
    ```powershell
    .\gradlew.bat devices
    ```

## Non-binary policy (recommended GitHub approach)

This repository uses a **source-only on-branch** policy and treats release binaries separately.

**Chosen default:** use **GitHub Releases** for distribution, while keeping Git history binary-free.

- Commit source, Gradle scripts, docs, and CI workflows.
- Do **not** commit APK/AAB/signing files to Git history.
- Publish APKs as **release assets** from CI on version tags.

If your organization interprets policy as “no binaries anywhere on GitHub,” keep this repo source-only and distribute via Play Console/Firebase/another artifact repository instead.

### Install setup (what you need to do locally)

If `gradle/wrapper/gradle-wrapper.jar` is missing, install Gradle on your machine and regenerate the wrapper JAR locally:

```bash
gradle wrapper --gradle-version 8.7 --no-validate-url
```

This recreates `gradle/wrapper/gradle-wrapper.jar` so `./gradlew` commands can run.

If your system uses `gradle8` or another command name, use that command instead.


## CI and release flow

Workflow: `.github/workflows/android-release.yml`

- On pull requests and pushes to `main`: runs `test`, `lint`, and `assembleDebug`.
- On tags like `v1.0.0`: builds `app-release-unsigned.apk` and publishes it to GitHub Releases.

> Note: the uploaded APK is unsigned by default. For production delivery, sign inside CI using encrypted GitHub Secrets (never commit keystores).
