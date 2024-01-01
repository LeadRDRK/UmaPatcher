# UmaPatcher
<img align="right" width="96" height="96" src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp">

UmaPatcher is an Uma Musume: Pretty Derby translation patching tool for Android. It allows you to
translate parts of the game into a different language by modifying the game's asset files.

# Requirements
- Any device(*) running Android 7.0 or above.
- At least 1.2GB of free space (The app will take around 300MB with the translation files downloaded)

\* For now, most of this app's features require root access. However, app patching does not require
root can and be used to install UI translations.

# Download
Download the latest release from the [Releases page](https://github.com/LeadRDRK/UmaPatcher/releases).

# Features
- Based on [umamusu-translate](https://github.com/noccu/umamusu-translate/tree/master) by noccu, with similar translation capabilities.
- Supports translating:
  - master.mdb (skills, names, missions, etc.)
  - Stories (character stories, race stories, etc.)
  - Home screen dialogs/interactions
  - Lyrics
  - UI translations via [Carrotless](https://github.com/LeadRDRK/Carrotless)
- Translation repo syncing: Allows you to quickly pull the latest translations using Git.
- Custom Git repo: You can use any Git repo that's a fork of umamusu-translate.
- Backup/restore asset files.

By default, the app also uses [a fork of umamusu-translate](https://github.com/LeadRDRK/umamusu-translate)
with up to date translations for the skill names/descriptions and character names/titles from GameTora.

### Planned
- General asset management tools.

# How to use
The app should be self-explanatory. On the Home screen, tap on the Patch button on any of the cards,
customize the options if needed then hit Confirm. The patching process should begin. You'll need to
repeat this for each part of the game you want to patch.

### General tips
- If you don't know anything about the options, it's best to leave them as default.
- **DO NOT** tab out of the app or lock the screen(*) while the app is patching or syncing. Your device
  might terminate the app to save resources, which might lead to data corruption.
- Backups are disabled for everything except master.mdb by default due to storage space concerns. If you want
  to be able to restore the original files later, remember to turn it on manually for each patcher. Note
  that it will take quite a bit of space with the story patcher.
- Be patient while the story patcher is running. It's gonna take a while since there are thousands of files that
  need to be patched.

\* The app will keep the screen on by itself, so your phone won't lock unless you do so manually.

## Patching the app
The app can be patched with [Carrotless](https://github.com/LeadRDRK/Carrotless) which brings new
features such as UI translations and tweaks to the game itself. You can pick whether to patch the
APK file of the already installed app, or select an APK file and patch it.

Once you've patched the app and installed it, you can use the **Install data** function to install
the UI translations.

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

# Updating
Simply tap on the sync icon on the top right of the Home screen to sync the latest changes from the
translation repo. To check for updates for the app itself, tap on the Check for updates button on the
About screen.

The app will sync the repo and occasionally check for updates whenever you open it by default.
You can turn these off in Settings.

# Extras
The source code for this app also includes UnityKt, a brand new Unity assets file manipulation library
written in Kotlin that was specifically made for this project. It is currently located in
[com.leadrdrk.umapatcher.unity](app/src/main/java/com/leadrdrk/umapatcher/unity)

# License
[Apache License 2.0](LICENSE)