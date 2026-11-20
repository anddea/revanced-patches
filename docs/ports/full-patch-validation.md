# Full default-patch compatibility validation

All ten requested versions pass complete Morphe Desktop CLI runs, including dex and resource
compilation. The final runs used normal compatibility selection and stopped on errors: no
`--force`, `--exclusive` or `--continue-on-error`. `--unsigned` keeps validation builds separate
from installable signed releases. The source build passed `./gradlew buildAndroid`.

| Target | Applied default patches | Failed patches | Rebuild | ZIP CRC |
| --- | ---: | ---: | --- | --- |
| music-6.20.51 | 44 | 0 | Passed | Passed |
| music-7.25.53 | 44 | 0 | Passed | Passed |
| music-8.30.54 | 43 | 0 | Passed | Passed |
| music-9.15.51 | 44 | 0 | Passed | Passed |
| music-9.40.51 | 44 | 0 | Passed | Passed |
| youtube-19.28.42 | 88 | 0 | Passed | Passed |
| youtube-20.05.46 | 88 | 0 | Passed | Passed |
| youtube-20.21.37 | 88 | 0 | Passed | Passed |
| youtube-21.13.164 | 88 | 0 | Passed | Passed |
| youtube-21.39.525 | 88 | 0 | Passed | Passed |

Music 8.30 has one fewer selected patch because its existing compatibility filters exclude
Third-party lyrics. Feature-specific guards for functionality absent from older apps remain in place.
The final outputs are under `/tmp/full-native-final/`; the compact results and bundle hash are in
`full-patch-validation.json` beside this document. These checks do not establish runtime behavior
on a device or extend Android OS support below the target metadata's existing minSdk policy.

## Fixes and rationale

- Music native downloads require 7.25+. The extension now checks an injected inclusion marker,
  rather than just the user's switch, so 6.20 cannot route into native hooks that were not installed.
  Music's existing Downloads patch also retains its older-version guard.
- Music 6.20's Guava Futures.submit builds tasks through a factory; newer versions use a constructor.
  Match the actual Futures class, exact Callable/Executor signature and executor invocation rather
  than requiring one allocation sequence. The media-provider device-file search fix is preserved.
- Music miniplayer views on older versions use the existing TOP_START/TOP_END IDs. Do not rename
  those public resource symbols: attrs.xml also needs them when the full resource table is rebuilt.
  Modern clients continue using the extension's dynamically resolved view IDs.
- Supply the missing generic summary string referenced by the Music native-download preference.
- Older YouTube More videos handlers already open the panel unconditionally. Match and verify that
  implementation when the newer conditional-state fingerprint is absent; do not disable Player components.
- YouTube 21.39 builds ordinary icon maps from pairs instead of EnumMap.put. Hook the completed
  map store, copy the map before inserting Cairo/Watch later icons, and support the renamed bell
  resource. The old EnumMap hooks remain for older implementations.
- The account-network-error hook follows Morphe's two-controller matching rather than assuming
  fixed method parameters. Match the stable error string instead of a removed drawable resource.
- Bundle the SponsorBlock skip-next vector explicitly, since the old stock drawable was removed.
- Register the newly validated Music 6.20/7.25 and YouTube 19.28/20.21 versions as experimental
  compatibility entries. This lets normal CLI selection apply the patches without forcing them.

## Reproduction

```sh
./gradlew buildAndroid
MORPHE_DATA_DIR=/tmp/full-patch-validation-data java -Xmx8g \
  -jar ../morphe-desktop-2.1.1-all.jar patch --unsigned \
  -p patches/build/libs/patches-4.4.0-dev.6.mpp \
  -t /tmp/full-patch-validation-work -o /tmp/full-patch-validation.apk \
  -r /tmp/full-patch-validation.json /path/to/original.apk
```

Inspect `failedPatches` and every `stepResults` entry, not just the report's top-level success
field: diagnostic runs with `--continue-on-error` can write an APK despite patch failures.
The final results here contain zero failed patches and successful PATCHING and REBUILDING steps.
YouTube 21.39's validation input was base.apk extracted from the supplied local APKM; configuration
splits were not merged into a universal APK. No output was installed or tested on a device.
