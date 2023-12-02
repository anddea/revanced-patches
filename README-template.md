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
    "name": "Default video quality",
    "description": "Adds ability to set default video quality settings.",
    "compatiblePackages":[
      {
        "name":"com.google.android.youtube",
        "versions":[
          "18.25.40",
          "18.27.36",
          "18.29.38",
          "18.30.37",
          "18.31.40",
          "18.32.39",
          "18.33.40",
          "18.34.38",
          "18.35.36",
          "18.36.39",
          "18.37.36",
          "18.38.44",
          "18.39.41",
          "18.40.34",
          "18.41.39",
          "18.42.41",
          "18.43.45",
          "18.44.41",
          "18.45.43",
          "18.46.43"
        ]
      }
    ],
    "use":true,
    "requiresIntegrations":false,
    "options": []
  },
  {
    "name": "Remember video quality",
    "description": "Save the video quality value whenever you change the video quality.",
    "compatiblePackages": [
      {
        "name": "com.google.android.apps.youtube.music",
        "versions": [
          "6.21.52+"
        ]
      }
    ],
    "use":true,
    "requiresIntegrations":false,
    "options": []
  }
]
```