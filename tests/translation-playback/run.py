#!/usr/bin/env python3
"""Run deterministic playback tests and optional PIT mutations without an Android runtime."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import urllib.request

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
PACKAGE = 'app.morphe.extension.youtube.patches.voiceovertranslation'
RELATIVE_PACKAGE = Path(PACKAGE.replace('.', '/'))
MAIN = ROOT / 'extensions/shared/src/main/java'
TEST = ROOT / 'extensions/shared/src/test/java'
DEPENDENCIES = [
    ('junit/junit/4.13.2/junit-4.13.2.jar', '8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3'),
    ('org/hamcrest/hamcrest-core/1.3/hamcrest-core-1.3.jar', '66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9'),
    ('org/pitest/pitest/1.30.0/pitest-1.30.0.jar', '73f561066a9b681611356db417cc8e7bdb1deec8e564239ee3ce8720bab64cec'),
    ('org/pitest/pitest-entry/1.30.0/pitest-entry-1.30.0.jar', '1902c50bcdd6db79176ee59ad28c4d4c1a13805bdeaaba010cff97d5f0ea7b19'),
    ('org/pitest/pitest-command-line/1.30.0/pitest-command-line-1.30.0.jar', 'a7ee242c8da89be731cb2c32993500aa552049a68735af6928862ed258fed009'),
    ('org/apache/commons/commons-text/1.15.0/commons-text-1.15.0.jar', '58d2da30f058512a1e7f914e39241deca4dff5c27a085b4ed2faa9e7208067f6'),
    ('org/apache/commons/commons-lang3/3.20.0/commons-lang3-3.20.0.jar', '69e5c9fa35da7a51a5fd2099dfe56a2d8d32cf233e2f6d770e796146440263f4'),
]
EXTRACTED = {
    'GooglePauseMethods': ('GoogleVoiceOverTranslationPatch', [
        'startAutomaticTranslation', 'clearNativeStartupAudio', 'checkStartupReady',
        'onPlaybackPaused', 'onVoiceChanged', 'reloadTranscript', 'suspendTranslation',
        'nativeStartupFinished', 'newVideoLoaded', 'toggleTranslation',
    ]),
    'YandexPauseMethods': ('VoiceOverTranslationPatch', [
        'clearPausedVideoState', 'invalidateTranslationRequest', 'isCurrentTranslationRequest',
        'isCurrentTranslationRequestGeneration', 'pauseVideoForTranslation',
        'resumeVideoAfterTranslationReady', 'startTranslationRequest',
        'shouldPlayTranslationAudio', 'startAutomaticTranslation',
    ]),
}
# Only deterministic stand-ins and the two logging lambdas are excluded. Production pause
# state, controller, and extracted provider methods all use PIT's ALL mutator set.
BOUNDARY_METHODS = [
    'ensureTts', 'stopTts', 'resetPlaybackState', 'notifyStateChanged',
    'updateTranslationActiveCache', 'resolveTargetLang', 'resolveVoice',
    'loadTranscript', 'updateTtsLanguage', 'logError', 'lambda$checkStartupReady*',
    'lambda$nativeStartupFinished$1',
    'clearTranslationRequestProgress', 'notifyTranslationStateChanged',
    'requestTranslation', 'isTranslationActive', 'isTranslationRequestInProgress',
]


def extract_method(source, name):
    """Read the actual checked-out method on every run; never maintain a test copy."""
    match = re.search(r'    (?:public |private |protected |static )*(?:void|boolean) '
                      + re.escape(name) + r'\([^)]*\) \{', source)
    if match is None:
        raise ValueError(f'Production method not found: {name}')
    start = match.start()
    end = source.index('{', start) + 1
    depth = 1
    # These selected methods contain no braces inside string literals/comments.
    # Fail at extraction/compilation if their source shape changes.
    while depth:
        if source[end] == '{':
            depth += 1
        elif source[end] == '}':
            depth -= 1
        end += 1
    return source[start:end].replace('private static', 'static')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mutation', action='store_true')
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--output', type=Path, default=ROOT / 'build/translation-playback-tests')
    args = parser.parse_args()
    java = str(Path(args.java_home) / 'bin/java') if args.java_home else 'java'
    javac = str(Path(args.java_home) / 'bin/javac') if args.java_home else 'javac'
    output = args.output.resolve()
    dependencies = output / 'dependencies'
    generated = output / 'generated'
    classes = output / 'classes'
    for directory in (dependencies, generated, classes):
        directory.mkdir(parents=True, exist_ok=True)
    jars = []
    for coordinate, expected_hash in DEPENDENCIES[:None if args.mutation else 2]:
        path = dependencies / Path(coordinate).name
        if not path.exists():
            with urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + coordinate) as response:
                path.write_bytes(response.read())
        if hashlib.sha256(path.read_bytes()).hexdigest() != expected_hash:
            raise ValueError(f'Dependency checksum mismatch: {path.name}')
        jars.append(path)
    for target, (origin, methods) in EXTRACTED.items():
        source = (MAIN / RELATIVE_PACKAGE / f'{origin}.java').read_text()
        body = '\n'.join(extract_method(source, method) for method in methods)
        template = (HERE / f'{target}.java.in').read_text()
        (generated / f'{target}.java').write_text(template.replace('// PRODUCTION_METHODS', body))
    preference = (MAIN / 'app/morphe/extension/shared/settings/preference/AbstractPreferenceFragment.java').read_text()
    listener = re.search(r'    private final SharedPreferences.OnSharedPreferenceChangeListener listener = .*?\n    };', preference, re.S)
    if listener is None:
        raise ValueError('Production preference listener not found')
    callback = extract_method((MAIN / 'app/morphe/extension/youtube/settings/preference/YouTubePreferenceFragment.java').read_text(), 'onSettingChanged')
    template = (HERE / 'PreferenceChangeMethods.java.in').read_text()
    (generated / 'PreferenceChangeMethods.java').write_text(template.replace('// PRODUCTION_LISTENER', listener.group()).replace('// PRODUCTION_CALLBACK', callback))
    source_files = list((HERE / 'fixtures').rglob('*.java')) + list(generated.glob('*.java'))
    source_files += [MAIN / RELATIVE_PACKAGE / f'{name}.java' for name in
                     ('TranslationPlaybackState', 'TranslationPlaybackController')]
    source_files.append(TEST / RELATIVE_PACKAGE / 'TranslationPlaybackStateTest.java')
    classpath = os.pathsep.join(map(str, [classes, *jars]))
    subprocess.run([javac, '-g', '-cp', classpath, '-d', str(classes), *map(str, source_files)], check=True)
    targets = ['TranslationPlaybackState', 'TranslationPlaybackController', *EXTRACTED]
    tests = [PACKAGE + '.' + target + 'Test' for target in [*targets, 'PreferenceChangeMethods']]
    subprocess.run([java, '-cp', classpath, 'org.junit.runner.JUnitCore', *tests], check=True)
    if args.mutation:
        # Exercise the two integration seams that a controller-only mutant cannot cover.
        preference_source = generated / 'PreferenceChangeMethods.java'
        original = preference_source.read_text()
        mutations = {
            'missing-dispatch': ('if (!settingImportInProgress) onSettingChanged(setting);', ''),
            'stale-cache': ('Setting.privateSyncValueFromPreferences(setting);', ''),
        }
        try:
            for name, (before, after) in mutations.items():
                if original.count(before) != 1:
                    raise ValueError(f'Preference mutation no longer matches: {name}')
                preference_source.write_text(original.replace(before, after))
                subprocess.run([javac, '-g', '-cp', classpath, '-d', str(classes), str(preference_source)], check=True)
                result = subprocess.run([java, '-cp', classpath, 'org.junit.runner.JUnitCore', PACKAGE + '.PreferenceChangeMethodsTest'], capture_output=True, text=True)
                (output / f'preference-mutant-{name}.txt').write_text(result.stdout + result.stderr)
                if result.returncode == 0 or 'FAILURES!!!' not in result.stdout:
                    raise AssertionError(f'Preference mutant was not killed by assertions: {name}')
                print(f'Preference integration mutant killed: {name}', flush=True)
        finally:
            preference_source.write_text(original)
            subprocess.run([javac, '-g', '-cp', classpath, '-d', str(classes), str(preference_source)], check=True)
        subprocess.run([
            java, '-cp', classpath, 'org.pitest.mutationtest.commandline.MutationCoverageReport',
            '--reportDir', str(output / 'mutation-report'),
            '--targetClasses', ','.join(PACKAGE + '.' + target for target in targets),
            '--targetTests', ','.join(tests), '--sourceDirs', str(MAIN) + ',' + str(generated),
            '--mutators', 'ALL', '--threads', '4', '--outputFormats', 'XML,HTML',
            '--timestampedReports=false', '--avoidCallsTo', 'app.morphe.extension.shared.utils.Logger',
            '--excludedMethods', ','.join(BOUNDARY_METHODS),
        ], check=True)


if __name__ == '__main__':
    main()
