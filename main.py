import argparse
import os
import subprocess
import sys

# ANSI escape codes for colors
GREEN = '\033[92m'
RESET = '\033[0m'


def run_command(command, cwd=None):
    print(f"{GREEN}Running command:{RESET} {' '.join(command)}")
    result = subprocess.run(command, capture_output=True, text=True, cwd=cwd)
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print(f"Error: {result.stderr}")
    return result.returncode


def main():
    parser = argparse.ArgumentParser(description="Run various string processing scripts.")

    parser.add_argument('-a', '--all', action='store_true', help="Run all commands in order.")
    parser.add_argument('-m', '--missing', action='store_true', help="Run src/missing_strings.py.")
    parser.add_argument('-r', '--replace', action='store_true', help="Run src/replace_strings.py.")
    parser.add_argument('--remove', action='store_true', help="Run src/remove_unused_strings.py.")
    parser.add_argument('-s', '--sort', action='store_true', help="Run src/sort_strings.py.")

    parser.add_argument('--youtube', action='store_true',
                        help="Specify the --youtube argument for replace and sort commands.")
    parser.add_argument('--music', action='store_true',
                        help="Specify the --music argument for replace and sort commands.")
    parser.add_argument('--rvx-base-dir', type=str, help="Specify the base directory of RVX patches operations.",
                        required=True)

    args = parser.parse_args()

    sub_arg = '--music' if args.music else '--youtube'
    rvx_base_dir_arg = f'--rvx-base-dir={args.rvx_base_dir}' if args.rvx_base_dir else ''

    commands = []

    if args.all:
        commands = [
            [sys.executable, 'src/missing_strings.py'],
            [sys.executable, 'src/replace_strings.py', rvx_base_dir_arg],
            [sys.executable, 'src/replace_strings.py', '--music', rvx_base_dir_arg],
        ]

        # Change directory, git fetch, and git pull before remove_unused_strings.py
        current_dir = os.getcwd()
        rvx_dir = os.path.join(current_dir, args.rvx_base_dir)

        if run_command(['git', 'switch', 'dev'], cwd=rvx_dir) != 0:
            print("Error during git checkout")
            return
        if run_command(['git', 'fetch'], cwd=rvx_dir) != 0:
            print("Error during git fetch")
            return
        if run_command(['git', 'pull'], cwd=rvx_dir) != 0:
            print("Error during git pull")
            return

        commands.extend([
            [sys.executable, 'src/remove_unused_strings.py'],
            [sys.executable, 'src/sort_strings.py'],
            [sys.executable, 'src/sort_strings.py', '--music']
        ])
    else:
        if args.missing:
            commands.append([sys.executable, 'src/missing_strings.py'])
        if args.replace:
            commands.append([sys.executable, 'src/replace_strings.py', sub_arg])
        if args.remove:
            commands.append([sys.executable, 'src/remove_unused_strings.py'])
        if args.sort:
            commands.append([sys.executable, 'src/sort_strings.py', sub_arg])

    for command in commands:
        run_command(command)


if __name__ == "__main__":
    main()
