"""
These python scripts follow PEP 8 guidelines with the exception that the maximum line length is extended
to 120 characters.
All other PEP 8 conventions apply, such as naming conventions, indentation, imports, and documentation strings.
The project uses the Black formatter for consistent code style and Flake8 for linting and style checks.
"""

import argparse
import os
import subprocess
import sys

# ANSI escape codes for colors
GREEN = "\033[92m"
RESET = "\033[0m"


def run_command(command, cwd=None):
    print(f"{GREEN}Running command:{RESET} {' '.join(command)}")
    result = subprocess.run(command, capture_output=True, text=True, cwd=cwd)
    if result.stdout:
        print(result.stdout)
    if result.stderr:
        print(f"Error: {result.stderr}")
    return result.returncode


def git_operations(rvx_dir):
    """
    Perform git operations (checkout, fetch, pull) in the specified directory.
    """
    if run_command(["git", "switch", "dev"], cwd=rvx_dir) != 0:
        print("Error during git checkout")
        return False
    if run_command(["git", "fetch"], cwd=rvx_dir) != 0:
        print("Error during git fetch")
        return False
    if run_command(["git", "pull"], cwd=rvx_dir) != 0:
        print("Error during git pull")
        return False
    return True


def main():
    parser = argparse.ArgumentParser(description="Run various string processing scripts.")

    parser.add_argument("-a", "--all", action="store_true", help="Run all commands in order.")
    parser.add_argument(
        "-m",
        "--missing",
        action="store_true",
        help="Run missing_strings.py.",
    )
    parser.add_argument(
        "-r",
        "--replace",
        action="store_true",
        help="Run replace_strings.py.",
    )
    parser.add_argument(
        "--remove",
        action="store_true",
        help="Run remove_unused_strings.py.",
    )
    parser.add_argument("-s", "--sort", action="store_true", help="Run sort_strings.py.")
    parser.add_argument("-p", "--prefs", action="store_true", help="Run missing_prefs.py.")

    parser.add_argument(
        "--youtube",
        action="store_true",
        help="Specify the --youtube argument for replace and sort commands.",
    )
    parser.add_argument(
        "--music",
        action="store_true",
        help="Specify the --music argument for replace and sort commands.",
    )
    parser.add_argument(
        "--rvx-base-dir",
        type=str,
        help="Specify the base directory of RVX patches operations.",
    )

    args = parser.parse_args()

    # Retrieve the rvx_base_dir from environment variables if not provided as
    # an argument
    rvx_base_dir = args.rvx_base_dir or os.getenv("RVX_BASE_DIR")

    if not rvx_base_dir:
        raise ValueError(
            "rvx_base_dir must be specified either as an argument or through \
            the RVX_BASE_DIR environment variable."
        )

    sub_arg = "--music" if args.music else "--youtube"
    rvx_base_dir_arg = f"--rvx-base-dir={rvx_base_dir}"

    commands = []

    if args.all:
        if git_operations(rvx_base_dir):
            commands = [
                [sys.executable, "src/utils/replace_strings.py", "--youtube", rvx_base_dir_arg],
                [sys.executable, "src/utils/replace_strings.py", "--music", rvx_base_dir_arg],
                [sys.executable, "src/utils/missing_strings.py", "--youtube"],
                [sys.executable, "src/utils/missing_strings.py", "--music"],
                [sys.executable, "src/utils/remove_unused_strings.py", "--youtube"],
                [sys.executable, "src/utils/remove_unused_strings.py", "--music"],
                [sys.executable, "src/utils/sort_strings.py", "--youtube"],
                [sys.executable, "src/utils/sort_strings.py", "--music"],
            ]
    else:
        if args.missing:
            commands.append([sys.executable, "src/utils/missing_strings.py", sub_arg])
        if args.prefs:
            commands.append([sys.executable, "src/utils/missing_prefs.py", rvx_base_dir_arg])
        if args.remove:
            commands.append([sys.executable, "src/utils/remove_unused_strings.py", sub_arg])

        if args.replace:
            if git_operations(rvx_base_dir):
                commands.append([sys.executable, "src/utils/replace_strings.py", sub_arg, rvx_base_dir_arg])

        if args.sort:
            commands.append([sys.executable, "src/utils/sort_strings.py", sub_arg])

    for command in commands:
        run_command(command)


if __name__ == "__main__":
    main()
