"""Parse log files to extract function execution times and display performance statistics.

This module reads a log file containing function execution times, calculates statistical metrics
(count, min, avg, median, mode, max), and outputs the results in a Markdown-formatted table.
The log file is expected to have lines in the format '<function_name> took <time> ms'.
"""
# ruff: noqa: T201

from __future__ import annotations

import argparse
import math
import re
import statistics
import sys
from collections import Counter, defaultdict
from pathlib import Path

LOG_LINE_PATTERN = re.compile(r"^(\w+) took ([\d.]+) ms$")


def parse_log_data(log_file_path: str) -> dict[str, list[float]] | None:
    """Parse log file and extract function execution times.

    Args:
        log_file_path: Path to the log file.

    Returns:
        A dictionary mapping function names to lists of execution times, or None if an error occurs.

    """
    function_times: defaultdict[str, list[float]] = defaultdict(list)
    try:
        with Path(log_file_path).open() as f:
            for line_content in f:
                match = LOG_LINE_PATTERN.match(line_content.strip())
                if match:
                    func_name, time_str = match.groups()
                    try:
                        time_val = float(time_str)
                        function_times[func_name].append(time_val)
                    except ValueError:
                        continue
    except FileNotFoundError:
        print(f"Error: Log file '{log_file_path}' not found.", file=sys.stderr)
        return None
    except OSError as e:
        print(f"An error occurred while reading the file: {e}", file=sys.stderr)
        return None
    return function_times


def calculate_function_statistics(times: list[float]) -> dict[str, int | float]:
    """Calculate statistical metrics for a list of execution times.

    Args:
        times: List of execution times in milliseconds.

    Returns:
        A dictionary containing count, min, avg, median, mode, and max values.

    """
    if not times:
        return {
            "count": 0,
            "min": float("nan"),
            "avg": float("nan"),
            "median": float("nan"),
            "mode": float("nan"),
            "max": float("nan"),
        }

    count = len(times)
    min_val = min(times)
    max_val = max(times)
    avg_val = statistics.mean(times)
    median_val = statistics.median(times)

    try:
        mode_val = statistics.mode(times)
    except statistics.StatisticsError:
        counts = Counter(times)
        if not counts:
            mode_val = float("nan")
        else:
            max_freq = max(counts.values())
            modes = sorted([val for val, freq in counts.items() if freq == max_freq])
            mode_val = modes[0] if modes else float("nan")

    return {
        "count": count,
        "min": min_val,
        "avg": avg_val,
        "median": median_val,
        "mode": mode_val,
        "max": max_val,
    }


def print_statistics_table_markdown(all_function_stats: dict[str, dict[str, int | float]]) -> None:
    """Print function performance statistics in a Markdown table.

    Args:
        all_function_stats: Dictionary mapping function names to their statistics.

    """
    if not all_function_stats:
        print("No data to display.")
        return

    headers = ["Function Name", "Count", "Min (ms)", "Avg (ms)", "Median (ms)", "Mode (ms)", "Max (ms)"]
    alignments = [0, 1, 1, 1, 1, 1, 1]  # 0 for left, 1 for right

    col_widths = [len(h) for h in headers]
    data_rows: list[list[str]] = []

    for func_name, stats in sorted(all_function_stats.items()):
        row = [func_name, str(stats["count"])]
        for key in ["min", "avg", "median", "mode", "max"]:
            val = stats[key]
            row.append("N/A" if math.isnan(float(val)) else f"{val:.4f}")
        data_rows.append(row)

        for idx, cell in enumerate(row):
            col_widths[idx] = max(col_widths[idx], len(cell))

    # Print header
    header_line = [f"{h:<{w}}" if a == 0 else f"{h:>{w}}" for h, w, a in zip(headers, col_widths, alignments)]
    print("| " + " | ".join(header_line) + " |")

    # Print separator
    separator_line = [f"{'-' * max(3, w)}" for w in col_widths]
    print("| " + " | ".join(separator_line) + " |")

    # Print data rows
    for row in data_rows:
        data_line = [f"{cell:<{w}}" if a == 0 else f"{cell:>{w}}" for cell, w, a in zip(row, col_widths, alignments)]
        print("| " + " | ".join(data_line) + " |")


def main_script_runner(log_file_path: str) -> int:
    """Run the main script to parse and display log file statistics.

    Args:
        log_file_path: Path to the log file.

    Returns:
        Exit code (0 for success, 1 for failure).

    """
    function_data = parse_log_data(log_file_path)

    if function_data is None:
        return 1

    if not function_data:
        print("No performance data found in the log file.")
        return 0

    all_function_stats: dict[str, dict[str, int | float]] = {}
    for func_name, times_list in function_data.items():
        all_function_stats[func_name] = calculate_function_statistics(times_list)

    print_statistics_table_markdown(all_function_stats)
    return 0


if __name__ == "__main__":
    """Command-line interface for parsing log file performance statistics."""
    parser = argparse.ArgumentParser(description="Parse log file and display function performance statistics.")
    parser.add_argument("-f", "--file", required=True, help="Path to the log file.", metavar="<path_to_log_file>")

    args = parser.parse_args()
    sys.exit(main_script_runner(args.file))
