# UmaPatcher
<img align="right" width="96" height="96" src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp">

[![Discord server](https://dcbadge.limes.pink/api/server/https://discord.gg/BVEt5FcxEn)](https://discord.gg/BVEt5FcxEn)
[![ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/Q5Q3QEHRA)

UmaPatcher is an UM:PD Hachimi mod patching tool for Android. Its main purpose is to provide you
with translation patches within the game, among other features.

For more information, please check out [Hachimi](https://github.com/Hachimi-Hachimi/Hachimi).

# Requirements
- Android 7.0 or above (***EXCEPT*** for Android 11, see [Hachimi#3](https://github.com/Hachimi-Hachimi/Hachimi/issues/3))

# Download
Download the latest release from the [Releases page](https://github.com/LeadRDRK/UmaPatcher/releases).

# How to use
## Patching the app
The app can be patched with [Hachimi](https://github.com/Hachimi-Hachimi/Hachimi) which brings new
features such as translations and tweaks to the game itself. You can pick whether to patch the
APK file of the already installed app, or select an APK file and patch it.

### Rooted
You can use the **Direct install** option on rooted devices. This will mount a modified APK file on
top on your existing one, which will keep the original app safe and does not require reinstalling the
app.

### Non-root
You must choose to **save the patched APK file** and install it manually. If you've already had the
app installed before, you must uninstall it before installing the patched APK, because it is signed
with a different key. The signing key is reused the next time you patch another one, so you can update
the app later on. This also means that once you've started using it, everytime there's a new update,
you have to patch the updated APK to able to install it.

**Note 1:** If you've already had an account on the game and you don't want to lose it when reinstalling
the app, you can use the game's Data Link feature.

**Note 2:** The signing key is unique for each installation of UmaPatcher. It is also a self-signed
key; during installation, Google Play Protect will warn you of installing an untrusted app. **You
can safely ignore this and tap on More Info -> Install anyways to continue installation.**

## Setting up Hachimi
Just open the patched app that you've installed and follow Hachimi's first time setup wizard.

**Tip:** You can access the menu by pressing the Volume up + Volume down keys on your device at the
same time.

# License
[Apache License 2.0](LICENSE)
