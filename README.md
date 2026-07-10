# Magica

Privilege Escalation PoC when seccomp is disabled for Android 10 - Android 16.

[![Android CI](https://github.com/vvb2060/Magica/actions/workflows/android.yml/badge.svg)](https://github.com/vvb2060/Magica/actions/workflows/android.yml)

```cpp
static void com_android_internal_os_Zygote_nativeInstallSeccompUidGidFilter(
        JNIEnv* env, jclass, jint uidGidMin, jint uidGidMax) {
  if (!gIsSecurityEnforced) {
    ALOGI("seccomp disabled by setenforce 0");
    return;
  }

  bool installed = install_setuidgid_seccomp_filter(uidGidMin, uidGidMax);
  if (!installed) {
      RuntimeAbort(env, __LINE__, "Could not install setuid/setgid seccomp filter.");
  }
}
```
https://github.com/aosp-mirror/platform_frameworks_base/commit/86f08a5190c8a36497ff3b9848ce3e6d0ba2e951

Before Android 17, if SELinux is permissive, setuid seccomp filter would not be installed, allowing the app zygote to set any UID. 

```java
/**
 * Manages the UID transition policy for the safesetid Linux Security Module.
 *
 * <p>This class is used to control which UID transitions are permitted for isolated service
 * processes that are spawned from an application zygote. It interacts with the safesetid policy
 * file in the kernel's securityfs.
 *
 * <p>The number of entries in the safesetid policy file should be equal to the number of running
 * apps using app zygote plus the number of *currently starting* isolated processes. A single
 * persistent entry is used per app zygote to lock-down UID transitions. One temporary entry is
 * added to allow the new process to set its UID and is removed from the policy once the new
 * isolated process has finished initialization.
 *
 * <p>This class is thread-safe. Since the underlying policy file exists in securityfs,
 * no FileLock is needed to ensure exclusive access.
 *
 * @hide
 */
public final class UidTransitionPolicy {

    private static final String TAG = "UidTransitionPolicy";
    private static final Path POLICY_FILE_PATH =
            Paths.get("/sys/kernel/security/safesetid/uid_allowlist_policy");
```
https://github.com/aosp-mirror/platform_frameworks_base/commit/fa9b62a3d926b6c63a0960c8b83244eb400b2c4

Android 17 uses SafeSetID to enforce UID policies.

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
