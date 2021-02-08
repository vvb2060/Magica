# Magica

Privilege Escalation PoC when seccomp is disabled for Android 10+.


## Usage

```sh
adb root
adb shell setenforce 0
adb shell stop
adb shell start
./gradlew :app:iR
```
[CI Artifacts](https://github.com/vvb2060/Magica/actions?query=branch%3Amaster)

## License

Public domain
