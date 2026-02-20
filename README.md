# OverlandTracker

## Gradle wrapper policy

This repository follows a **no-binary policy** for source control.

- `gradle/wrapper/gradle-wrapper.jar` is intentionally **not committed**.
- `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.properties` are kept in git.

### Regenerate wrapper artifacts locally

If your environment has Gradle installed, run:

```bash
gradle wrapper --gradle-version 8.7 --no-validate-url
```

This recreates `gradle/wrapper/gradle-wrapper.jar` for local use.
