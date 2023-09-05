## 🧩 ReVanced Patches

ReVanced Extended Patches.

## 📋 List of patches in this repository

{{ table }}

## 📝 JSON Format

This section explains the JSON format for the [patches.json](patches.json) file.

Example:

```json
[
  {
    "name": "default-video-quality",
    "description": "Adds ability to set default video quality settings.",
    "excluded": false,
    "options": [],
    "dependencies": [],
    "compatiblePackages": [
      {
        "name": "com.google.android.youtube",
        "versions": [
          "18.19.36",
          "18.20.39",
          "18.21.35",
          "18.22.37",
          "18.23.36",
          "18.24.37",
          "18.25.40",
          "18.27.36",
          "18.29.38",
          "18.30.37",
          "18.31.40"
        ]
      }
    ]
  },
  {
    "name": "remember-video-quality",
    "description": "Save the video quality value whenever you change the video quality.",
    "excluded": false,
    "options": [],
    "dependencies": [],
    "compatiblePackages": [
      {
        "name": "com.google.android.apps.youtube.music",
        "versions": [
          "6.15.52",
          "6.17.52"
        ]
      }
    ]
  }
]
```