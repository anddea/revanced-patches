#!/usr/bin/env python3
"""Exercise actual compatibility methods, their JVM API signatures, and PIT ALL mutations."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import runpy
import subprocess
import urllib.request

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[1]
MAIN = ROOT / 'extensions/shared/src/main/java/app/morphe/extension'


def method(source, name):
    match = re.search(r'    (?:public |private |static )*(?:String|void) ' + name + r'\([^)]*\) \{', source)
    if not match:
        raise ValueError('Missing production method: ' + name)
    start = match.start()
    end = source.index('{', start) + 1
    depth = 1
    # These method bodies contain no braces inside literals or comments.
    while depth:
        depth += (source[end] == '{') - (source[end] == '}')
        end += 1
    return source[start:end].replace('private ', '')


def compatible_signatures(bytecode):
    return all(signature in bytecode for signature in (
        'Matcher.appendReplacement:(Ljava/lang/StringBuffer;Ljava/lang/String;)',
        'Matcher.appendTail:(Ljava/lang/StringBuffer;)',
        'URLEncoder.encode:(Ljava/lang/String;Ljava/lang/String;)',
    )) and 'URLEncoder.encode:(Ljava/lang/String;Ljava/nio/charset/Charset;)' not in bytecode


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
    parser.add_argument('--mutation', action='store_true')
    args = parser.parse_args()
    def tool(name):
        return str(Path(args.java_home) / 'bin' / name) if args.java_home else name
    output = ROOT / 'build/android-compatibility-tests'
    generated, classes, dependencies = [output / part for part in ('generated', 'classes', 'dependencies')]
    for directory in (generated, classes, dependencies):
        directory.mkdir(parents=True, exist_ok=True)
    coordinates = runpy.run_path(str(HERE.parent / 'translation-playback/run.py'))['DEPENDENCIES']
    jars = []
    for coordinate, digest in coordinates[:None if args.mutation else 2]:
        path = dependencies / Path(coordinate).name
        if not path.exists():
            with urllib.request.urlopen('https://repo.maven.apache.org/maven2/' + coordinate) as response:
                path.write_bytes(response.read())
        if hashlib.sha256(path.read_bytes()).hexdigest() != digest:
            raise ValueError('Dependency checksum mismatch: ' + path.name)
        jars.append(path)
    prompt = (MAIN / 'music/patches/lyrics/OpenAIClient.java').read_text()
    title = (MAIN / 'youtube/patches/originaltitles/OriginalTitleRequest.java').read_text()
    panel = (MAIN / 'music/patches/lyrics/ui/LyricsPanelView.java').read_text()
    constants_start = prompt.index('    public static final String DEFAULT_PROMPT')
    constants_end = prompt.index(';', prompt.index('private static final Pattern PROMPT_VARIABLE')) + 1
    body = prompt[constants_start:constants_end] + '\n' + '\n'.join((
        method(prompt, 'renderPrompt'), method(prompt, 'promptValue'),
        method(title, 'fetchTitle'), method(panel, 'suppressLyricsLayout'),
    ))
    source = (HERE / 'CompatibilityMethods.java.in').read_text().replace('// PRODUCTION_METHODS', body)
    target = generated / 'CompatibilityMethods.java'
    target.write_text(source)
    classpath = os.pathsep.join(map(str, [classes, *jars]))
    def compile_target():
        subprocess.run([tool('javac'), '-g', '-cp', classpath, '-d', str(classes), str(target)], check=True)
        return subprocess.check_output([tool('javap'), '-c', '-p', '-classpath', str(classes), 'CompatibilityMethods'], text=True)
    bytecode = compile_target()
    if not compatible_signatures(bytecode):
        raise AssertionError('Methods use APIs unavailable at the supported minimum Android versions')
    test = HERE / 'CompatibilityMethodsTest.java'
    subprocess.run([tool('javac'), '-g', '-cp', classpath, '-d', str(classes), str(test)], check=True)
    subprocess.run([tool('java'), '-cp', classpath, 'org.junit.runner.JUnitCore', 'CompatibilityMethodsTest'], check=True)
    if args.mutation:
        # PIT cannot substitute an overload; restore each incompatible call as a seeded mutant.
        for before, after in (
            ('StringBuffer out = new StringBuffer', 'StringBuilder out = new StringBuilder'),
            ('StandardCharsets.UTF_8.name()', 'StandardCharsets.UTF_8'),
        ):
            assert before in source
            try:
                target.write_text(source.replace(before, after))
                assert not compatible_signatures(compile_target()), 'Incompatible overload escaped'
            finally:
                target.write_text(source)
        compile_target()
        print('Both seeded incompatible API overloads rejected.', flush=True)
        subprocess.run([
            tool('java'), '-cp', classpath, 'org.pitest.mutationtest.commandline.MutationCoverageReport',
            '--reportDir', str(output / 'mutation-report'), '--targetClasses', 'CompatibilityMethods',
            '--targetTests', 'CompatibilityMethodsTest', '--sourceDirs', str(generated),
            '--mutators', 'ALL', '--threads', '2', '--outputFormats', 'XML,HTML',
            '--timestampedReports=false', '--excludedMethods', 'lambda$fetchTitle$*',
            '--avoidCallsTo', 'CompatibilityBoundaries$Logger',
        ], check=True)


if __name__ == '__main__':
    main()
