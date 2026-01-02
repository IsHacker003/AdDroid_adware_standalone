# AdDroid Adware
This app is an adware* made using Google's Admob SDK, written completely in Kotlin. It can display ads on boot, on unlocking the screen, on installing/removing an app, etc. **For educational purposes only**.

On Android 10+, you will need to enable "Display over other apps" permission for the app for the ads to be displayed, due to Android's background activity start restrictions (I'm trying to find an exploit to get around this).

*Adware is a type of malware which displays unwanted, intrusive and annoying ads to users, usually in the form of pop-ups. Most adware also act as spyware by collecting personal information for ad (re)targeting.


**NOTE:** This is the standalone version of the AdDroid adware. For the SDK, go to [AdDroid_adware_SDK repo](https://github.com/IsHacker003/AdDroid_adware_SDK).

## Features
1. Displays ads on receiving multiple intents: `RECEIVE_BOOT_COMPLETED`,`USER_PRESENT`, `PACKAGE_FULLY_REMOVED`, etc.
2. Undetectable in launcher and hard to find in apps list, as it has no name and logo
3. The app itself is open source (except for Google Admob SDK which is proprietary)
4. Ads are either displayed as full screen interstitials or sent through notifications
5. Very small APK size (~3MB)
6. The adware gets activated only after the user reboots after installing the app
7. Full-screen interstitial ads will be constantly shown while in the settings app, making it painful to uninstall the adware through Settings > Apps (**this behavior was tested on AOSP Android 13**)
8. When an ad is closed, all apps will be minimized and the launcher/homescreen will be displayed, making the adware extremely annoying

## Download
APK available in [Releases](https://github.com/IsHacker003/AdDroid_adware_standalone/releases).

## Compiling
The APK can be compiled normally using Android Studio. The app uses test ads, if you want to show real ads then replace the `AD_UNIT_ID` variable inside `Constants.kt` with your own interstitial ad unit ID (but your account will probably get banned). **Do not use the app for illegal purposes**.

## Ad blockers
When the app is not able to connect to `googleads.g.doubleclick.net` to load the ad due to an ad blocker app, firewall or DNS service, the main service (AdsService.kt) is killed, so that empty popup windows are not displayed. Instead, another service (AdblockBypassService.kt) is started, which tries to load the ad every second (as a bonus, this also slows down the device). The second the ad blocker has been disabled, this service loads the ad and starts the main service again, causing the ads to appear.

A new implementation to bypass ad blockers completely is also under way. It will use DNS-over-HTTPS (DoH) to bypass the block.
