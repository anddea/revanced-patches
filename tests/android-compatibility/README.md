# Android compatibility regression tests

Requires Python 3 and JDK 21. Run from the repository root:

```sh
python3 tests/android-compatibility/run.py --java-home "$JAVA_HOME"
python3 tests/android-compatibility/run.py --java-home "$JAVA_HOME" --mutation
```

The runner extracts the actual `renderPrompt`, `promptValue`, `fetchTitle` and
`suppressLyricsLayout` methods on every run. JUnit and PIT dependencies use the
same checksum-pinned coordinates as the translation playback tests. Generated
sources and reports stay under `build/android-compatibility-tests`.

The tests check literal replacement characters, unknown and repeated variables,
null/default inputs, UTF-8 URL encoding, response handling, timeouts, retry timing,
and layout suppression at API 26–28 versus API 29 and later. The network, clock,
Android version and view are deterministic boundaries; no live request is sent.
Bytecode signature checks reject the newer `StringBuilder` Matcher overloads and
the `Charset` URLEncoder overload. Both are also restored as seeded regressions
to prove that the signature checks catch them.

The compatibility limits come from the app targets, not just the extension's
minimum SDK: `COMPATIBILITY_YOUTUBE` supports 21.13.164 at API 28, and
`COMPATIBILITY_YOUTUBE_MUSIC_THIRD_PARTY_LYRICS` supports 9.15.51 at API 26.
`suppressLayout` requires API 29, the Matcher StringBuilder overloads API 34, and
the URLEncoder Charset overload API 33.

16 regression tests pass. PIT ALL produces 113 mutations: 111 killed, two
reviewed survivors, zero uncovered. The survivors change only the initial
StringBuffer capacity (`+256` to `+257`, or removing the template length); neither
changes the output because the buffer grows automatically. Only the diagnostic
logging lambdas are excluded from the production-method target. Boundary classes
are outside that target.

The narrowly scoped drawing lint suppressions preserve existing behavior:
framework TextViews retain the host app's styling, and `drawBaseText` is called
only from `onDraw`, where its `super.onDraw` fallback is valid. Replacing that
fallback with `draw` would re-enter the override. This harness does not test
Android rendering; the full Gradle build and lint check validate the source
integration, while visual acceptance would require a YouTube Music device test.
