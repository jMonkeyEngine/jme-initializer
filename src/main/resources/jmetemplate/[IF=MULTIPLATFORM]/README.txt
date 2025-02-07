# [GAME_NAME_FULL]

This is the readme for [GAME_NAME_FULL], try to keep it up to date with any information future-you will wish past-you
remembered to write down

## Project set up
This is a gradle project using JMonkey Engine and other java libraries

# Modules : 

Game module `:game` : holds `build.gradle` dependencies for the game code & should hold your code.

[IF=JME_DESKTOP]Desktop module `:desktop` : holds `build.gradle` for desktop dependencies & implements the `:game` module, this module can hold the desktop gui.
[/IF=JME_DESKTOP]
[IF=JME_ANDROID]Android module `:app` : holds `build.gradle` for the android dependencies & implements the `:game` module, this module can hold android dependent gui.
[/IF=JME_ANDROID]

# Running Game : 
[IF=JME_DESKTOP]
### Desktop : 

```gradle
./gradlew run
```
[/IF=JME_DESKTOP]
[IF=JME_ANDROID]
### Android : 

Install the app on a connected device via adb.

```bash
    $./gradlew :app:installDebug
```
If you are using Android Studio launch and debug options will be available directly within the IDE

[/IF=JME_ANDROID]

# Building Game :

### Desktop :
[IF=JME_DESKTOP]
```bash
    $./gradlew :desktop:copyJars
```
[/IF=JME_DESKTOP]
[IF=JME_ANDROID]
### Android : 
```gradle
    $./gradlew :app:assemble
```
[/IF=JME_ANDROID]

Note that if you are releasing your app on the android play store it must be signed with an appropriate key, see
https://developer.android.com/studio/build/building-cmdline for more details on how to produce such apks

[FRAGMENT=gradleDeploymentReadme.fragment]

References : 

=> gradlew for android:
[IF=JME_ANDROID]https://developer.android.com/studio/build/building-cmdline[/IF=JME_ANDROID]

=> Gradle DSL : https://docs.gradle.org/current/dsl/index.html

=> Gradle for java : https://docs.gradle.org/current/userguide/multi_project_builds.html

=> Gradle/Groovy Udacity course by google : https://github.com/udacity/ud867/blob/master/1.11-Exercise-ConfigureFileSystemTasks/solution.gradle

[IF=JME_ANDROID]
=> See JMonkeyEngine Android Examples : https://github.com/Scrappers-glitch/jme3-Simple-Examples

https://github.com/Scrappers-glitch/DBTraining

https://github.com/Scrappers-glitch/Superior-Extended-Engine/tree/master/demoApp
[/IF=JME_ANDROID]
[IF=JME_DESKTOP]
=> See JMonkeyEngine Desktop Example : https://github.com/Scrappers-glitch/basic-gradle-template

=> See JMonkeyEngine RPI armhf Desktop Example : https://github.com/Scrappers-glitch/JmeCarPhysicsTestRPI
[/IF=JME_DESKTOP]

## txt vs md

This readme is provided as a .txt as that is a common format openable on any machine. However, it would more normally be a .md, this will allow it to be nicely formatted by most git repositories (assuming you commit it to git). Just change the extension from .txt to .md, the syntax is already correct for an md file
