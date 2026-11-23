# Installing Bee Timer

Bee Timer is an Android app. It is not in the Play Store. You download one file from GitHub and open it.
It takes about five minutes. It needs Android 12 or newer.

The wording of Android's own questions differs a little from phone to phone. The steps are the same.

## Install

1. On your phone, open this page in your browser:
   https://github.com/MechEng70/beetimer/releases/latest
2. Under "Assets", tap the file that ends in `.apk` (for example `beetimer-0.1.0-alpha.2.apk`). If the browser
   warns that the file might be harmful, choose to download it anyway.
3. When the download is done, tap "Open" (or tap the file in your notifications or your Downloads).
4. The first time, Android says your browser is not allowed to install apps. Tap "Settings", turn on
   "Allow from this source", then go back.
5. Tap "Install", then "Open".

You only do step 4 once.

## Allow the alarms

Bee Timer rings when a timed step ends, often with the phone in your pocket and the screen off. Android only
lets that happen if you allow it.

- **Notifications:** shows each alarm and the list of active hives on your lock screen.
- **Alarms and reminders:** rings at the exact time, not "around then".
- **Full-screen alerts:** shows the alarm over the lock screen. Some phones turn this off for sideloaded apps.
- **No battery limits:** keeps alarms from being delayed. The app asks once.

Alarms ring at your **alarm volume**, so they work on silent and in Do Not Disturb. If your alarm volume is
low when you start the first hive, Bee Timer shows "Alarm volume is low" with a Raise button for a few seconds.

If alarms come late or not at all, check the battery settings for Bee Timer in your phone's settings.

## Updating

Install the new version the same way, over the old one. Do not uninstall first, or your hives and settings
are lost.

**Coming from a test build (0.1.0-alpha):** builds before 0.1.0-alpha.1 were signed with a test key, and
Android won't install a release over them ("App not installed" or "package conflicts"). Uninstall the old one
first, then install. This loses its hives and settings, so note your hive names first. It's needed once; later
releases install over 0.1.0-alpha.1 and keep everything.

## From a computer (adb)

With USB debugging on and the phone plugged in:

```
adb install -r beetimer-0.1.0-alpha.2.apk
```
