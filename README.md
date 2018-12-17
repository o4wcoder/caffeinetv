# Mitsurugi (Android)

[![Build Status](https://travis-ci.com/caffeinetv/android.svg?token=qBfqv76hyjADhUsCTEp6&branch=master)](https://travis-ci.com/caffeinetv/android)

The Android app for Caffeine.

<img alt="Mitsurugi" src="logo.png" width="356">

### Setup

1. Install Android Studio 3.3 RC 2 (latest 3.3 version)
2. Configure an emulator.
3. Run unit tests.
4. Run android tests.

### About

* Development is done in Kotlin and Android Studio
  * Current version of Kotlin is 1.3.11
  * Current version of Android Studio is 3.3 RC 2
* The app uses the following libraries:
  * AndroidX, JetPack, Support Libraries
    * AppCompat
    * Material design
    * Navigation
  * Dagger
  * Retrofit
  * OkHttp
  * Picasso
* The app uses WebRTC for Android
  * Must use version 1.0.24277
  * Versions 1.0.24465, 1.0.24616 and later cause crashes
* Play Services

### Releasing

* The app signing is managed by Google Play Store ([managed app sigining](https://support.google.com/googleplay/android-developer/answer/7384423))
* Creating a build:
  * From 1Password, download the Mitsurugi key store
  * In Android Studio select the Build - Generate Signed Bundle/APK menu
  * Choose "Android App Bundle" and click Next
  * Enter the path to the Mitsurugi key store, enter "mitsurugi" as the key alias
  * Enter the key store and key password from 1Password
  * Click Next

### Tools

* [Dexcount Gradle Plugin](https://github.com/KeepSafe/dexcount-gradle-plugin)


### Digital Items

* Currently (as of Sep 28, 2018), [SceneForm does not support FBX animations](https://github.com/google-ar/sceneform-android-sdk/issues/11)


### Library Issues

The currently usable version of WebRTC is 1.0.24277. Using later versions results in a crash:

```
2018-10-11 12:12:39.271 15892-15986/tv.caffeine.app.debug A/libc: Fatal signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x6c in tid 15986 (Thread-25), pid 15892 (feine.app.debug)
2018-10-11 12:12:39.345 16028-16028/? I/crash_dump64: obtaining output fd from tombstoned, type: kDebuggerdTombstone
2018-10-11 12:12:39.345 953-953/? I//system/bin/tombstoned: received crash request for pid 15986
2018-10-11 12:12:39.347 16028-16028/? I/crash_dump64: performing dump of process 15892 (target tid = 15986)
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: *** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: Build fingerprint: 'google/taimen/taimen:9/PPR2.181005.003/4984323:user/release-keys'
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: Revision: 'rev_10'
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: ABI: 'arm64'
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: pid: 15892, tid: 15986, name: Thread-25  >>> tv.caffeine.app.debug <<<
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: signal 11 (SIGSEGV), code 1 (SEGV_MAPERR), fault addr 0x6c
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG: Cause: null pointer dereference
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x0  0000007f0ca8c000  x1  0000000000000000  x2  0000007f0b2a4b31  x3  0000007f0b2a4f08
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x4  0000000000000000  x5  0000007f0edf7100  x6  000000000000007d  x7  000000000000007d
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x8  0000000000000000  x9  79e3af7eb06f7b51  x10 0000000000000000  x11 0000007f0ca8c018
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x12 00000000ffffffff  x13 000000000000000b  x14 0000007f0e200000  x15 0000000000000000
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x16 0000007f0d1f0610  x17 0000007faeb83a58  x18 000000005bbfa0a7  x19 0000007f0e3a7000
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x20 0000007f0b2a4f08  x21 0000007f0edf7100  x22 0000000000000000  x23 0000007f0e3a7208
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x24 0000007f1034c200  x25 0000007f0b2a4b31  x26 0000007f1034c200  x27 0000000000000048
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     x28 0000000002a14b99  x29 0000007f0ca8c060
2018-10-11 12:12:39.354 16028-16028/? A/DEBUG:     sp  0000007f0ca8c000  lr  0000007f0cf3e270  pc  0000007f0cf3e274
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG: backtrace:
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #00 pc 00000000003b3274  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #01 pc 00000000003b28e8  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #02 pc 0000000000525380  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #03 pc 0000000000525a64  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #04 pc 00000000002ae550  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.355 16028-16028/? A/DEBUG:     #05 pc 00000000002bf7ec  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.356 16028-16028/? A/DEBUG:     #06 pc 00000000002addb0  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.356 16028-16028/? A/DEBUG:     #07 pc 00000000002bf520  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.356 16028-16028/? A/DEBUG:     #08 pc 00000000002bf428  /data/app/tv.caffeine.app.debug-F9uk3Pqj_QsaU-z_-boZSw==/lib/arm64/libjingle_peerconnection_so.so (offset 0x225000)
2018-10-11 12:12:39.356 16028-16028/? A/DEBUG:     #09 pc 0000000000083114  /system/lib64/libc.so (__pthread_start(void*)+36)
2018-10-11 12:12:39.356 16028-16028/? A/DEBUG:     #10 pc 00000000000233bc  /system/lib64/libc.so (__start_thread+68)
2018-10-11 12:12:40.453 953-953/? E//system/bin/tombstoned: Tombstone written to: /data/tombstones/tombstone_06
2018-10-11 12:12:40.454 1201-16034/? W/ActivityManager:   Force finishing activity tv.caffeine.app.debug/tv.caffeine.app.MainActivity
```

