# Mitsurugi (Android)

[![CircleCI](https://circleci.com/gh/caffeinetv/android.svg?style=svg&circle-token=55b5ed44af44b4352f6b3d466030f1f40fb00582)](https://circleci.com/gh/caffeinetv/android)

The Android app for Caffeine.

<img alt="Mitsurugi" src="logo.png" width="356">

### First-Time Setup

1. Checkout the repo:
  ```
  git clone git@github.com:caffeinetv/android.git
  ```
2. Go to the project directory:
  ```
  cd android
  ```
3. Install Git LFS:
  ```
  brew install git-lfs
  git lfs install
  git lfs pull
  ```
4. Install ktlint, pre-commit hook, and apply to IDE:
  ```
  brew install ktlint
  ktlint --install-git-pre-commit-hook
  ktlint --apply-to-idea-project --android
  ```
5. Run unit tests:
  ```
  ./gradlew testDebugUnitTest
  ```
6. Open the project in Android Studio

### About

* Development is done in  Kotlin and Android Studio
  * Use latest released version of Kotlin (currently 1.3.31)
  * Use latest production or beta version of Android Studio (currently 3.5)
* CI is performed on CircleCI
  * Unit tests - also runs Detekt
  * Danger - checks PR size, presence of tests
  * Test coverage - uses JaCoCo
* The app uses modern Android libraries, such as:
  * AndroidX, JetPack
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
* The app is released to the internal track automatically when a PR is approved and merged to master.
  * CircleCI runs the `publish-to-play` job, which uses [gradle play publisher](https://www.github.com/Triple-T/gradle-play-publisher)
* Manual steps:
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

