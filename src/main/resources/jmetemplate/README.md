# [GAME_NAME_FULL]

This is the readme for [GAME_NAME_FULL], try to keep it up to date with any information future-you will wish past-you
remembered to write down

## Project set up
This is a gradle project using JMonkey Engine and other java libraries

# Modules : 

Game module `:game` : holds `build.gradle` dependencies for the game code & should hold your code.

[IF=DESKTOP]Desktop module `:desktop` : holds `build.gradle` for desktop dependencies & uses the `:game` module, this module can hold the desktop gui.
[/IF=DESKTOP]
[IF=ANDROID]Android module `:app` : holds `build.gradle` for the android dependencies & uses the `:game` module, this module can hold android dependent gui.
[/IF=ANDROID]
[IF=PC_VR] PC VR module `:pc-vr` : holds `build.gradle` for the PC VR dependencies & uses the `:game` module. This holds the launcher to launch the game in VR when tethered (either via a cable or wireless) to a PC
[/IF=PC_VR]
[IF=ANDROID_VR] Android VR module `:pc-vr` : holds `build.gradle` for the Android VR dependencies & uses the `:game` module. This holds the launcher to launch the game in VR when running locally on an android device (e.g. the Quest 3)
[/IF=ANDROID_VR]
# Running Game : 
[IF=DESKTOP]
### Desktop : 

```gradle
./gradlew run
```
[/IF=DESKTOP]
[IF=ANDROID]
### Android : 

Install the app on a connected device via adb.

```bash
    $./gradlew :app:installDebug
```
If you are using Android Studio launch and debug options will be available directly within the IDE

[/IF=ANDROID]

# Building Game :

### Desktop :
[IF=DESKTOP_OR_PC_VR]
```bash
    $./gradlew :desktop:copyJars
```
[/IF=DESKTOP_OR_PC_VR]
[IF=ANDROID_OR_ANDROID_VR]
### Android : 
```gradle
    $./gradlew :app:assemble
```

Note that if you are releasing your app on the android play store it must be signed with an appropriate key, see
https://developer.android.com/studio/build/building-cmdline for more details on how to produce such apks
[/IF=ANDROID_OR_ANDROID_VR]

[FRAGMENT=gradleDeploymentReadme.fragment]

References :


[IF=ANDROID]
=> gradlew for android:
https://developer.android.com/studio/build/building-cmdline[/IF=ANDROID]

=> Gradle DSL : https://docs.gradle.org/current/dsl/index.html

=> Gradle for java : https://docs.gradle.org/current/userguide/multi_project_builds.html

=> Gradle/Groovy Udacity course by google : https://github.com/udacity/ud867/blob/master/1.11-Exercise-ConfigureFileSystemTasks/solution.gradle

[IF=ANDROID]
=> See JMonkeyEngine Android Examples : https://github.com/Scrappers-glitch/jme3-Simple-Examples

https://github.com/Scrappers-glitch/DBTraining

https://github.com/Scrappers-glitch/Superior-Extended-Engine/tree/master/demoApp
[/IF=ANDROID]
[IF=DESKTOP]
=> See JMonkeyEngine Desktop Example : https://github.com/Scrappers-glitch/basic-gradle-template

=> See JMonkeyEngine RPI armhf Desktop Example : https://github.com/Scrappers-glitch/JmeCarPhysicsTestRPI
[/IF=DESKTOP]

[IF=PC_VR_OR_ANDROID_VR]
=> See Tamarin Wiki for VR guidance: https://github.com/oneMillionWorlds/Tamarin/wiki
[/IF=PC_VR_OR_ANDROID_VR]