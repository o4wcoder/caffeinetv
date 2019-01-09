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
* WebRTC for Android
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

