# OverlandTracker

## Gradle wrapper policy

This repository follows a **no-binary policy**.

- `gradle/wrapper/gradle-wrapper.jar` is intentionally **not committed**.
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` are committed.
- Prebuilt wrapper binaries are **not distributed by this repository**.

### Install setup (what you need to do locally)

If `gradle/wrapper/gradle-wrapper.jar` is missing, install Gradle on your machine and regenerate the wrapper JAR locally:

```bash
gradle wrapper --gradle-version 8.7 --no-validate-url
```

This recreates `gradle/wrapper/gradle-wrapper.jar` so `./gradlew` commands can run.

If your system uses `gradle8` or another command name, use that command instead.


## Android app releases

This repository can publish an APK to GitHub Releases via
`.github/workflows/android-release.yml`.

- Trigger automatically when pushing a tag that starts with `v` (for example `v1.0.0`).
- Or run manually from **Actions** and provide a `tag` value.
- The workflow builds `app-release-unsigned.apk` and uploads it to the release.

> Note: the uploaded APK is unsigned. Sign the APK in a follow-up step if you need an installable production artifact.
