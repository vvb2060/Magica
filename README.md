# Magica

Privilege Escalation PoC when seccomp is disabled for Android 10+.

[![Android CI](https://github.com/vvb2060/Magica/actions/workflows/android.yml/badge.svg)](https://github.com/vvb2060/Magica/actions/workflows/android.yml)

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
