# Mitsurugi (Android)

[![Build Status](https://travis-ci.com/caffeinetv/android.svg?token=qBfqv76hyjADhUsCTEp6&branch=master)](https://travis-ci.com/caffeinetv/android) [![CircleCI](https://circleci.com/gh/caffeinetv/android.svg?style=svg)](https://circleci.com/gh/caffeinetv/android)

The Android app for Caffeine.

<img alt="Mitsurugi" src="logo.png" width="356">

### Setup

1. Install Android Studio 3.4
2. Configure an emulator.
3. Run unit tests.
4. Run android tests.

### About

* Development is done in Kotlin and Android Studio
  * Current version of Kotlin is 1.3.20
  * Current version of Android Studio is 3.4
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
* [Danger](https://danger.systems)

### Digital Items

* [SceneForm finally supports FBX animations](https://developers.google.com/ar/develop/java/sceneform/animation/)

