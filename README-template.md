## 🧩 ReVanced Patches

ReVanced Extended Patches.

## Documentation

Check the [wiki](https://github.com/anddea/revanced-patches/wiki) for resources on patching, customization, and debugging.

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
    "compatiblePackages":[
      {
        "name": "com.google.android.youtube",
        "versions": "COMPATIBLE_PACKAGE_YOUTUBE"
      }
    ],
    "use":true,
    "options": []
  },
  {
    "name": "Bitrate default value",
    "description": "Sets the audio quality to 'Always High' when you first install the app.",
    "compatiblePackages": [
      {
        "name": "com.google.android.apps.youtube.music",
        "versions": "COMPATIBLE_PACKAGE_MUSIC"
      }
    ],
    "use":true,
    "options": []
  },
  {
    "name": "Hide ads",
    "description": "Adds options to hide ads.",
    "compatiblePackages": [
      {
        "name": "com.reddit.frontpage",
        "versions": "COMPATIBLE_PACKAGE_REDDIT"
      }
    ],
    "use":true,
    "options": []
  }
]
```