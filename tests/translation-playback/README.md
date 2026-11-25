# Translation playback regression and mutation tests

Requires Python 3 and JDK 21. The first run downloads checksum-pinned JUnit and Kotlin compiler dependencies from Maven Central. Mutation runs also download PIT 1.30.0 and its dependencies. Subsequent runs reuse the verified cache under `build/translation-playback-tests`.

```sh
python3 tests/translation-playback/run.py --java-home "$JAVA_HOME"
python3 tests/translation-playback/run.py --java-home "$JAVA_HOME" --mutation
```

The normal Android unit suite also runs `TranslationPlaybackStateTest` through `:extensions:shared:testDebugUnitTest`.

## Scope

- Compile the actual `TranslationPlaybackState` and `TranslationPlaybackController` sources against deterministic Android/player/provider boundaries.
- Extract the actual Yandex and Google pause-related method bodies from the current checkout on every run. The generated classes change only the enclosing class and private method visibility; the bodies are not maintained as test copies.
- Extract both Yandex prepared-audio callbacks, the native playback-state reader, and the shared Google audio player's start/resume/speed methods. Model an accepted play command that leaves the video idle, delayed speech preparation, stale callbacks, and Android's implicit start when playback parameters change. Voice previews remain independent of video playback.
- Extract Yandex synchronization, speed updates, completion listeners and replay/resume methods. Model Android restarting completed audio when nonzero playback parameters are applied, including completion between state checks, delayed resume ticks, shorter/longer translation tracks, replay, backward seeks and replacement players. Verify completion-listener registration in both Yandex paths and the shared native/Edge TTS player.
- Extract the Yandex request/poll/failure paths with a deterministic service boundary, and compile the actual Kotlin click, activation and accessibility methods. Test cancellation during loading, preparation and recreation, error recovery, stale failures after retry, per-video suppression, and automatic translation on the next video.
- Exercise automatic-only startup holds, unselected manual playback, manual-request recreation, pre-request settings changes, suppressed autoplay recovery on a new manual-mode video, metadata ownership, manual play/pause, completion during player recreation, duplicate metadata, provider switching, cancellation/failure, automatic dispatch, watch-page loading, both Google speech engines, voice changes, transcript replacement, native synthesis completion, stale audio reads, and same/new-video transitions.
- Exercise the actual preference listener and YouTube callback against storage/UI stand-ins, including cache synchronization before dispatch. These 11 integration tests are outside the PIT pause-method target set.
- Mutate all compiled pause methods with PIT's `ALL` operators. Include generated request-handler and accessibility classes. Exclude test classes, deterministic boundary implementations, harness entry points and two logging lambdas, listed explicitly in `run.py`. Kotlin methods are mutated as JVM bytecode; this does not use a Kotlin-specific mutation plugin.

The fixtures control scheduling, synthesis completion, cached audio, player state and request generations. They do not test Android services, network requests, binary patch injection, or real speech playback. Those require device validation. Native and Edge engines share the Google pause logic but follow separate readiness branches in these tests.

## Baseline result

349 tests passed. Two additional seeded preference-integration mutations (missing playback dispatch and stale setting cache) are killed by assertions. PIT generated 2,017 mutations: 1,948 killed, 69 survived, none uncovered. Reports are generated at `build/translation-playback-tests/mutation-report/`.

The surviving mutations were reviewed, not hidden by a percentage threshold:

| Area | Count | Why the behavior tests do not distinguish them |
| --- | ---: | --- |
| `GooglePauseMethods` | 20 | Existing Google startup/player null, state, and logging branches. Removing a defensive check reaches an existing caught failure or an unobservable dormant value; these paths were not changed by the Yandex recovery work. |
| Controller, state, native/shared audio | 13 | `TranslationPlaybackController` / `TranslationPlaybackState` guards and native/shared-audio null handling retain the same caught-failure or stopped result. Deferred state values are overwritten by valid transitions. |
| `YandexAudioMethods` | 16 | Speed clamps that assign an already bounded value, request-identity guards, and seek/pause/resume guards. The tests exercise delayed, duplicate, replacement, canceled, and failed seeks; the surviving condition forms do not add a new observable failure. |
| `YandexPauseMethods` | 17 | Prepared-callback identity checks, seek/pause/resume guards, and the automatic duplicate-request guard. Delayed and canceled starts remain blocked, and a second playback failure still uses the final failure path. |
| `YandexRequestMethods` | 2 | Dereferencing a null response or audio URL reaches the existing catch block and generation-checked failure. |
| Kotlin generated null check | 1 | `Class.getName()` cannot return null; removing the compiler-inserted check does not change accessibility. |

Four of the surviving conditions are in `startPlaybackAfterSeek` (two per extracted class), covering its initial pending-player guard and failed-seek cleanup. The delayed, duplicate, replacement, cancellation, and failed-seek tests execute these lines, but those particular rewrites do not produce a distinct failing assertion.

A surviving mutation is not proof of correctness. Keep these limits visible, and re-review the XML report when the implementation or fixtures change.
