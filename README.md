# UmaPatcher
<img align="right" width="96" height="96" src="app/src/main/res/mipmap-xxhdpi/ic_launcher.webp">

UmaPatcher is an Uma Musume: Pretty Derby translation patching tool for Android. It allows you to
translate parts of the game into a different language by modifying the game's asset files.

# Requirements
- A **rooted** device(*) running Android 7.0 and above.
- At least 1.2GB of free space (The app will take around 300MB with the translation files downloaded)

\* Most of this app's features require root access to actually be useful. You can patch the master.mdb
file without root, but that's about all it can do, and you can't even install it into the game. *Unless
your device is rooted, this app is basically useless.*

# Download
Download the latest release from the [Releases page](https://github.com/LeadRDRK/UmaPatcher/releases).

# Features
- Based on [umamusu-translate](https://github.com/noccu/umamusu-translate/tree/master) by noccu, with similar translation capabilities.
- Supports translating:
  - master.mdb (skills, names, missions, etc.)
  - Stories (character stories, race stories, etc.)
  - Home screen dialogs/interactions
  - Lyrics
- Translation repo syncing: Allows you to quickly pull the latest translations using Git.
- Custom Git repo: You can use any Git repo that's a fork of umamusu-translate.
- Backup/restore asset files.

By default, the app also uses [a fork of umamusu-translate](https://github.com/LeadRDRK/umamusu-translate)
with up to date translations for the skill names/descriptions and character names/titles from GameTora.

### Planned
- General asset management tools.

### Not planned (for now)
- UI translations.

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

# Updating
Simply tap on the sync icon on the top right of the Home screen to sync the latest changes from the
translation repo. To check for updates for the app itself, tap on the Check for updates button on the
About screen.

The app will sync the repo and occasionally check for updates whenever you open it by default.
You can turn these off in Settings.

# License
[Apache License 2.0](LICENSE)