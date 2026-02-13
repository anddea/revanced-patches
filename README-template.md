<div align="center"> 
<img src="assets/rvx-logo.png" alt="RVX logo" width="128">

    
## 🧩 ReVanced Extended Patches
[![Static Badge](https://img.shields.io/badge/RVX_Wiki-gray?logo=github)](https://github.com/anddea/revanced-patches/wiki)   [![Static Badge](https://img.shields.io/badge/Translations-gray?logo=crowdin)](https://rvxtranslate.netlify.app/)
<br>
[![Static Badge](https://img.shields.io/badge/Telegram-Community-gray?logo=telegram&color=%2326A5E4)](https://t.me/AnddeaChat)   [![Static Badge](https://img.shields.io/badge/Reddit-RVX-gray?logo=reddit&color=red)](https://reddit.com/r/revancedextended)   [![Static Badge](https://img.shields.io/badge/Reddit-YTAdvanced-gray?logo=reddit&color=yellow)](https://www.reddit.com/r/YTadvanced)
</div>

## Documentation

Check the [wiki](https://github.com/anddea/revanced-patches/wiki) for resources on patching, customization, and debugging.

Report issues [here](https://github.com/inotia00/ReVanced_Extended).

This app uses code from Morphe. To learn more, visit https://morphe.software.

## 📋 List of patches in this repository

{{ table }}

## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

Example:

```json
[
  {
    "name": "Alternative thumbnails",
    "description": "Adds options to replace video thumbnails using the DeArrow API or image captures from the video.",
    "use":true,
    "compatiblePackages": {
      "com.google.android.youtube": "COMPATIBLE_PACKAGE_YOUTUBE"
    },
    "options": []
  },
  {
    "name": "Bitrate default value",
    "description": "Sets the audio quality to 'Always High' when you first install the app.",
    "use":true,
    "compatiblePackages": {
      "com.google.android.apps.youtube.music": "COMPATIBLE_PACKAGE_MUSIC"
    },
    "options": []
  },
  {
    "name": "Hide ads",
    "description": "Adds options to hide ads.",
    "use":true,
    "compatiblePackages": {
      "com.reddit.frontpage": "COMPATIBLE_PACKAGE_REDDIT"
    },
    "options": []
  }
]
```
