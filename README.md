# OverlandTracker

## Gradle wrapper policy

This repository keeps binaries out of normal source control history.

- `gradle/wrapper/gradle-wrapper.jar` is intentionally **not committed**.
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` are kept in git.
- `gradle-wrapper.jar` is published as a **GitHub Release asset** instead.

### Download wrapper JAR from Releases

Use the helper script to download the wrapper JAR into `gradle/wrapper/`:

```bash
scripts/download-gradle-wrapper-jar.sh --repo <owner/repo> --tag <release-tag>
```

Example:

```bash
scripts/download-gradle-wrapper-jar.sh --repo acme/OverlandTracker --tag gradle-wrapper-v8.7
```

By default the script fetches `gradle-wrapper.jar` and writes it to
`gradle/wrapper/gradle-wrapper.jar`.

### Publish wrapper JAR to Releases

This repo includes a workflow at `.github/workflows/publish-gradle-wrapper.yml`.
Run it manually to upload your local `gradle/wrapper/gradle-wrapper.jar` as a
release asset for a tag (for example `gradle-wrapper-v8.7`).


## Android app releases

This repository can publish an APK to GitHub Releases via
`.github/workflows/android-release.yml`.

- Trigger automatically when pushing a tag that starts with `v` (for example `v1.0.0`).
- Or run manually from **Actions** and provide a `tag` value.
- The workflow builds `app-release-unsigned.apk` and uploads it to the release.

> Note: the uploaded APK is unsigned. Sign the APK in a follow-up step if you need an installable production artifact.
