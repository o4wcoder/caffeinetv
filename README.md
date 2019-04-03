# Mitsurugi (Android)

[![CircleCI](https://circleci.com/gh/caffeinetv/android.svg?style=svg&circle-token=55b5ed44af44b4352f6b3d466030f1f40fb00582)](https://circleci.com/gh/caffeinetv/android)

The Android app for Caffeine.

<img alt="Mitsurugi" src="logo.png" width="356">

### First-Time Setup

1. Checkout the repo `git clone git@github.com:caffeinetv/android.git && cd android`
2. Install Git LFS: `brew install git-lfs && git lfs install && git lfs pull`
3. Run unit tests: `./gradlew testProdDebugUnitTest`
4. Open the project in Android Studio

### About

* Development is done in Kotlin and Android Studio
  * Current version of Kotlin is 1.3.21
  * Current version of Android Studio is 3.4
* CI is performed on CircleCI
  * Unit tests - also runs Detekt
  * Danger
  * Test coverage
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

* [Danger](https://danger.systems) - automated code review chores
* [Detekt](https://arturbosch.github.io/detekt/index.html) - static code analysis
* [JaCoCo](https://www.jacoco.org/jacoco/) - code coverage
* [Dexcount Gradle Plugin](https://github.com/KeepSafe/dexcount-gradle-plugin)

### Digital Items

* [SceneForm finally supports FBX animations](https://developers.google.com/ar/develop/java/sceneform/animation/)

