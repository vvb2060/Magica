# Magica

Privilege Escalation PoC when seccomp is disabled for Android 10+.
Confirmed to work for up to Android 16.
The "Install Magisk" button doesn't appear for Android 16, so there is likely some functional adjustments necessary. The rooting works though, and I was not curious about the rest. Refactoring it should be trivial.

![Android CI](https://github.com/vvb2060/Magica/workflows/Android%20CI/badge.svg?branch=master)

[Download (CI Artifacts)](https://github.com/vvb2060/Magica/actions?query=branch%3Amaster)

## Usage

```sh
adb root
adb shell setenforce 0
adb shell stop
adb shell start
./gradlew :app:iR
```

## License

Public domain
