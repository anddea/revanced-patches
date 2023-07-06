## 🧩 ReVanced Patches

The official ReVanced Extended Patches.

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
    "version": "0.0.1",
    "excluded": false,
    "options": [],
    "dependencies": [
      "settings"
    ],
    "compatiblePackages": [
      {
        "name": "com.google.android.youtube",
        "versions": [
          "18.20.39",
          "18.21.35",
          "18.22.37",
          "18.23.36",
          "18.24.37",
          "18.25.39"
        ]
      }
    ]
  }
]
```