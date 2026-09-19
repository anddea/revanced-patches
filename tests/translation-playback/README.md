# Translation playback regression and mutation tests

Requires Python 3 and JDK 21. The first run downloads checksum-pinned JUnit dependencies from Maven Central. Mutation runs also download PIT 1.30.0 and its dependencies. Subsequent runs reuse the verified cache under `build/translation-playback-tests`.

```sh
python3 tests/translation-playback/run.py --java-home "$JAVA_HOME"
python3 tests/translation-playback/run.py --java-home "$JAVA_HOME" --mutation
```

The normal Android unit suite also runs `TranslationPlaybackStateTest` through `:extensions:shared:testDebugUnitTest`.

## Scope

- Compile the actual `TranslationPlaybackState` and `TranslationPlaybackController` sources against deterministic Android/player/provider boundaries.
- Extract the actual Yandex and Google pause-related method bodies from the current checkout on every run. The generated classes change only the enclosing class and private method visibility; the bodies are not maintained as test copies.
- Exercise startup holds, metadata ownership, manual play/pause, completion during player recreation, duplicate metadata, provider switching, cancellation/failure, automatic dispatch, watch-page loading, both Google speech engines, voice changes, transcript replacement, native synthesis completion, stale audio reads, and same/new-video transitions.
- Exercise the actual preference listener and YouTube callback against storage/UI stand-ins, including cache synchronization before dispatch. These 11 integration tests are outside the PIT pause-method target set.
- Mutate all compiled pause methods with PIT's `ALL` operators. Exclude only test-boundary implementations and two logging lambdas, listed explicitly in `run.py`.

The fixtures control scheduling, synthesis completion, cached audio, player state and request generations. They do not test Android services, network requests, binary patch injection, or real speech playback. Those require device validation. Native and Edge engines share the Google pause logic but follow separate readiness branches in these tests.

## Baseline result

216 tests passed. Two additional seeded preference-integration mutations (missing playback dispatch and stale setting cache) are killed by assertions. PIT generated 1,034 mutations: 1,015 killed, 19 survived, none uncovered. Reports are generated at `build/translation-playback-tests/mutation-report/`.

The surviving mutations were reviewed, not hidden by a percentage threshold:

| Area | Count | Why the behavior tests do not distinguish them |
| --- | ---: | --- |
| State clears / guards | 7 | `resume` is not observable after `waiting` is cleared; selection/new-video logic overwrites it before another hold. The deferred-resume waiting check is defensive because every path that arms a hold clears the deferred value. |
| Controller guards | 2 | Retired directors are checked again inside the queued callback. The Google provider check in `pauseEnabled` is defensive for the valid ownership states exercised here; `NONE` cannot own a hold. |
| Google null checks | 5 | The receiver is already required by a preceding access or null guard. A missing duration still reaches the same caught failure when the explicit null check is removed. |
| Google main-thread checks | 3 | Calls deeper in the same operation enforce the main-thread requirement as well. |
| Google error logging | 2 | Removing the log does not change the failed-request release. |

A surviving mutation is not proof of correctness. Keep these limits visible, and re-review the XML report when the implementation or fixtures change.
