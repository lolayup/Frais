[简体中文](README.md) | English | [日本語](README_JP.md)

# Frais 雹

[![Android CI status](https://github.com/aistra0528/Frais/workflows/Android%20CI/badge.svg)](https://github.com/aistra0528/Frais/actions)
[![Translation status](https://hosted.weblate.org/widgets/frais/-/svg-badge.svg)](https://hosted.weblate.org/engage/frais/)
[![Downloads](https://img.shields.io/github/downloads/aistra0528/Frais/total.svg)](https://github.com/aistra0528/Frais/releases)
[![License](https://img.shields.io/github/license/aistra0528/Frais)](LICENSE)

Frais is a free-as-in-freedom software to freeze Android
apps. [GitHub Releases](https://github.com/aistra0528/Frais/releases)

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.khaled.frais/)

<img src="fastlane/metadata/android/en-US/images/phoneScreenshots/1.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/2.png" width="32%" /> <img src="fastlane/metadata/android/en-US/images/phoneScreenshots/3.png" width="32%" />

## Freeze

Freeze is a word that describes the action of **blocking (immediately stopping) apps when they are not needed/in-use (
on-demand request)** which in turn helps the device to cut down on the usage of RAM and save power. Users can also
unfreeze them to revert to their original state.

In general, "freeze" means disable, but also Frais can "freeze" apps by suspending them.

### Disable

Disabled apps will not be shown in the launcher and will be shown as "Disabled" in the installed apps list. Enable them
to revert the action.

### Suspend (Android 7.0+)

Suspended apps will have their icons shown in grayscale within the device's launcher. Unsuspend them to revert the
action.

> While in this state, the application's notifications will be hidden, any of its started activities will be stopped and
> it will not be able to show toasts, dialogs or even play audio. When the user tries to launch a suspended app, the
> system will, instead, show a dialog to the user informing them that they cannot use this app while it is suspended.

Suspend only prevents the user from interacting with the app, it does **NOT** prevent the app from running in the
background.

## Working mode

**Any app that has been frozen on Frais will need to be unfrozen by the same working mode.**

1. For devices supporting wireless debugging (Android 11+) or rooted devices, `Shizuku` is recommended.

2. For rooted devices, `Root` is an alternative. **It is slower.**

| Privilege                                                                                         | Force Stop | Disable | Suspend | Uninstall/Reinstall (System Apps) |
|---------------------------------------------------------------------------------------------------|------------|---------|---------|-----------------------------------|
| Root                                                                                              | ✓          | ✓       | ✓       | ✓                                 |
| Device Owner                                                                                      | ✗          | ✗       | ✓       | ✗                                 |
| Privileged System App                                                                             | ✓          | ✓       | ✗       | ✗                                 |
| [Shizuku](https://github.com/RikkaApps/Shizuku) (root)/[Sui](https://github.com/RikkaApps/Sui)    | ✓          | ✓       | ✓       | ✓                                 |
| [Shizuku](https://github.com/RikkaApps/Shizuku) (adb)                                             | ✓          | ✓       | ✓       | ✓                                 |
| [Dhizuku](https://github.com/iamr0s/Dhizuku)                                                      | ✗          | ✗       | ✓       | ✗                                 |
| [Island](https://github.com/oasisfeng/island)/[Insular](https://gitlab.com/secure-system/Insular) | ✗          | ✗       | ✓       | ✗                                 |

### Device Owner

**You must remove Frais as a device owner before you can uninstall it**

#### Set device owner by adb

[Android Debug Bridge (adb) Guide](https://developer.android.com/studio/command-line/adb)

[Download Android SDK Platform-Tools](https://developer.android.com/studio/releases/platform-tools)

Issue adb command:

```shell
adb shell dpm set-device-owner com.khaled.frais/.receiver.DeviceAdminReceiver
```

In response, adb prints this message if device owner has been successfully set:

```
Success: Device owner set to package com.khaled.frais. Active admin set to component {com.khaled.frais/com.khaled.frais.receiver.DeviceAdminReceiver}
```

Search the message by search engine otherwise.

#### Remove device owner

Settings > Remove Device Owner

### Privileged System App

The following privapp-permissions is required:

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.khaled.frais">
        <permission name="android.permission.PACKAGE_USAGE_STATS"/>
        <permission name="android.permission.FORCE_STOP_PACKAGES"/>
        <permission name="android.permission.CHANGE_COMPONENT_ENABLED_STATE"/>
        <permission name="android.permission.MANAGE_APP_OPS_MODES"/>
    </privapp-permissions>
</permissions>
```

To use this mode, you should install Frais as a privileged system app.

The recommended approach is to import Frais when building your ROM, here's an example for `Android.bp`:

```bp
android_app_import {
    name: "Frais",
    apk: "Frais.apk",
    privileged: true,

    dex_preopt: {
        enabled: false,
    },
    presigned: true,
    preprocessed: true,

    required: ["privapp-permissions_com.khaled.frais.xml"]
}

prebuilt_etc {
    name: "privapp-permissions_com.khaled.frais.xml",
    src: "privapp-permissions.xml",
    sub_dir: "permissions",
}
```

## Revert

### By adb

Replace com.package.name to the package name of target app.

```shell
# Enable app
adb shell pm enable com.package.name
# Unsuspend app
adb shell pm unsuspend com.package.name
```

### Modify file

Access `/data/system/users/0/package-restrictions.xml`, this file stores the restrictions about apps. You can modify,
rename or just delete it.

- Enable app: Modify the value of `enabled` from 2 (DISABLED) or 3 (DISABLED_USER) to 1 (ENABLED)

- Unsuspend app: Modify the value of `suspended` from true to false

### Wipe data by recovery

**None of my business :(**

## API

```shell
adb shell am start -a action -e key value
```

`action` can be one of the following constants:

- `com.khaled.frais.action.LAUNCH`: Unfreeze and launch target app. If it is unfrozen, it will launch directly.
  `key="package"` `value="com.package.name"`

- `com.khaled.frais.action.FREEZE`: Freeze target app. It must be checked at Home. `key="package"`
  `value="com.package.name"`

- `com.khaled.frais.action.UNFREEZE`: Unfreeze target app. `key="package"` `value="com.package.name"`

- `com.khaled.frais.action.FREEZE_TAG`: Freeze all non-whitelisted apps in the target tag. `key="tag"` `value="Tag name"`

- `com.khaled.frais.action.UNFREEZE_TAG`: Unfreeze all apps in the target tag. `key="tag"` `value="Tag name"`

- `com.khaled.frais.action.FREEZE_ALL`: Freeze all apps at Home. `extra` is not necessary.

- `com.khaled.frais.action.UNFREEZE_ALL`: Unfreeze all apps at Home. `extra` is not necessary.

- `com.khaled.frais.action.FREEZE_NON_WHITELISTED`: Freeze all non-whitelisted apps at Home. `extra` is not necessary.

- `com.khaled.frais.action.FREEZE_AUTO`: Auto freeze apps at Home. `extra` is not necessary.

- `com.khaled.frais.action.LOCK`: Lock screen. `extra` is not necessary.

- `com.khaled.frais.action.LOCK_FREEZE`: Freeze all apps at Home and lock screen. `extra` is not necessary.

or use following `schema`:

- `frais://launch?package=xxx`

- `frais://freeze?package=xxx`

- `frais://unfreeze?package=xxx`

- `frais://freeze_tag?tag=xxx`

- `frais://unfreeze_tag?tag=xxx`

- `frais://freeze_all`

- `frais://unfreeze_all`

- `frais://freeze_non_whitelisted`

- `frais://freeze_auto`

- `frais://lock`

- `frais://lock_freeze`

## Help Translate

To translate Frais into your language, or to improve an existing translation,
use [Weblate](https://hosted.weblate.org/engage/frais/).

[![Translation status](https://hosted.weblate.org/widgets/frais/-/multi-auto.svg)](https://hosted.weblate.org/engage/frais/)

## License

    Frais - Freeze Android apps
    Copyright (C) 2021-2026 Aistra
    Copyright (C) 2022-2026 Frais contributors

    This program is a free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
