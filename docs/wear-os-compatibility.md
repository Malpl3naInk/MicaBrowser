# CrossWalk on Wear OS 4

## Reproduced failures

Device: TicWatch Pro 5, Wear OS 4 / Android 13 (API 33), `armeabi-v7a`.
There is no system WebView provider on the tested firmware. MicaBrowser uses
the embedded `xwalk_core_library-23.53.589.4.aar`.

Opening the first page originally fails with:

```text
java.lang.NoClassDefFoundError: Failed resolution of: Ljunit/framework/Assert;
    at org.xwalk.core.XWalkCoreWrapper.attachXWalkCore
```

CrossWalk needs JUnit at runtime, so `testImplementation` alone is insufficient.
The application now packages `junit:junit:4.13.2` using `implementation`.

After fixing class loading, targeting API 33 exposes a separate native failure:

```text
avc: denied { open } for path="/dev/ashmem" ... scontext=u:r:untrusted_app:...
[FATAL:compositor_impl_android.cc(586)] Too many context creation failures. Giving up...
Fatal signal 6 (SIGABRT)
```

CrossWalk 23 directly opens the legacy ashmem device. A compatibility build
targeting API 28 uses Android's legacy application policy and successfully
renders on this firmware. No system components or SELinux settings are changed.

## Build and test

Use JDK 17, Android SDK platform 33, and the checked-in Gradle wrapper:

```sh
./gradlew -PlegacyCrosswalk=true assembleDebug assembleRelease
./gradlew -PlegacyCrosswalk=true connectedDebugAndroidTest
```

On Windows, use `gradlew.bat` instead. Select the device with `ANDROID_SERIAL`
when multiple devices are connected.

The default build still targets API 33. Only the explicit
`-PlegacyCrosswalk=true` option targets API 28; `compileSdk` and `minSdk` are
unchanged. This option applies to all variants in that Gradle invocation.

The device test launches `BrowserActivity` directly with self-contained pages,
checks JavaScript execution, navigates to another page, and goes back. It does
not depend on external network access or the in-progress launcher migration.
Also test a cold start of the standalone APK: an instrumentation runner can
otherwise mask a missing JUnit dependency by adding its own test libraries.

Verified on the device above: debug and minified release builds compile, and
the browser instrumentation test passes. A separate backport to `Rel-AL-0.4`
was also tested as a standalone minified APK by loading `https://example.com`.

## Limits

This is an opt-in sideload compatibility workaround, not an engine upgrade or
a Play Store target-API solution. CrossWalk 23 contains an old Chromium engine;
modern JavaScript, websites, and newer Android security requirements can still
be incompatible. A maintained rendering engine is the long-term solution.
