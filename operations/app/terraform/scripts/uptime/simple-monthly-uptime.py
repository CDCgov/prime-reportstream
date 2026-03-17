#!/usr/bin/env python3
# -*- coding: utf-8 -*-

""" Script to calculate monthly uptime based on the number of days in the month and the downtime duration.
    The inputs are year, month, and the downtime duration (e.g., "90m", "1.5h", "1h 30m", "45"), passed as command line arguments.
    If no unit ('h' or 'm') is provided, the duration is assumed to be in minutes.
    The script calculates the number of days in the month, the total number of hours in the month, and then calculates the uptime percentage.
    The output is a string formatted as "YYYY-MM: uptime%".
    The script handles leap years and months with different numbers of days.
    The script also handles invalid inputs gracefully, returning an error message if the inputs are not valid.

    The following expressions are valid:
    ./simple-monthly-uptime.py 2024 10 45         # 45 minutes downtime
    ./simple-monthly-uptime.py 2024 11 "1h 30m"   # 1 hour 30 minutes downtime
    ./simple-monthly-uptime.py 2025 2 "0.5h"      # Half an hour downtime
    ./simple-monthly-uptime.py 2025 3 "120m"      # 120 minutes downtime

"""

import sys
from calendar import monthrange
from typing import Tuple, Optional 
import argparse
import re 

def parse_duration_to_hours(duration_str: str) -> Optional[float]:
    """Parses a duration string (e.g., "1h 30m", "90m", "1.5h", "45") into hours.

    Args:
        duration_str: The duration string. Assumes minutes if no unit is specified.

    Returns:
        The total duration in hours as a float, or None if parsing fails.
    """
    duration_str = duration_str.strip().lower()
    total_hours = 0.0
    processed_indices = set() # Keep track of parts of the string already processed

    # Regex to find hours (e.g., "1h", "1.5 h", " 2 h ")
    hour_pattern = re.compile(r"(\d+(?:\.\d+)?)\s*h")
    # Regex to find minutes (e.g., "30m", "15 m", " 45 m ")
    minute_pattern = re.compile(r"(\d+(?:\.\d+)?)\s*m")

    # Find all hour matches
    for match in hour_pattern.finditer(duration_str):
        try:
            hours = float(match.group(1))
            if hours < 0: return None # Negative duration invalid
            total_hours += hours
            processed_indices.update(range(match.start(), match.end()))
        except (ValueError, IndexError):
            return None # Should not happen with regex, but safety first

    # Find all minute matches
    minutes_total = 0.0
    for match in minute_pattern.finditer(duration_str):
        # Avoid double-counting if part of an hour match (unlikely but possible with weird spacing)
        if any(i in processed_indices for i in range(match.start(), match.end())):
            continue
        try:
            minutes = float(match.group(1))
            if minutes < 0: return None # Negative duration invalid
            minutes_total += minutes
            processed_indices.update(range(match.start(), match.end()))
        except (ValueError, IndexError):
            return None

    total_hours += minutes_total / 60.0

    # Check for remaining unprocessed parts that are not just whitespace
    remaining_str = "".join(c for i, c in enumerate(duration_str) if i not in processed_indices).strip()

    # If there's remaining unprocessed string, check if it's the *entire* original string (meaning no units were found)
    if remaining_str:
        # Check if the *original* string contained *only* a number (default to minutes)
        if remaining_str == duration_str:
             try:
                 default_minutes = float(duration_str)
                 if default_minutes < 0: return None # Negative duration invalid
                 # If we are here, the entire string was just a number, treat as minutes
                 total_hours = default_minutes / 60.0
             except ValueError:
                 return None # Invalid format if remaining string is not the original number
        else:
            # If there's remaining text *after* processing h/m units, it's an invalid format
            return None

    # Final check if total_hours is negative (shouldn't happen with checks above, but good practice)
    if total_hours < 0:
        return None

    return total_hours


def calculate_uptime(year: int, month: int, downtime_hours: float) -> str:
    """Calculates the monthly uptime percentage.

    Args:
        year: The year.
        month: The month (1-12).
        downtime_hours: The number of hours the service was down.

    Returns:
        A string formatted as "YYYY-MM: uptime%".
        Returns an error message if the inputs are invalid.
    """
    if not (1 <= month <= 12):
        return "Error: Invalid month. Month must be between 1 and 12."
    # downtime_hours non-negative check is now implicitly handled by parse_duration_to_hours

    try:
        num_days = monthrange(year, month)[1]
        total_hours = num_days * 24

        # Add check: downtime cannot exceed total hours in the month
        if downtime_hours > total_hours:
             return f"Error: Downtime ({downtime_hours:.2f} hours) cannot exceed total hours in the month ({total_hours} hours)."

        uptime_hours = total_hours - downtime_hours
        # Handle potential floating point inaccuracies near 100%
        if uptime_hours >= total_hours:
             uptime_percentage = 100.0
        elif uptime_hours <= 0:
             uptime_percentage = 0.0
        else:
             uptime_percentage = (uptime_hours / total_hours) * 100

        return f"{year}-{month:02d}: {uptime_percentage:.2f}%"
    except ValueError:
        # This primarily catches invalid year/month for monthrange
        return "Error: Invalid year or month provided."


def parse_arguments() -> Tuple[int, int, str]: # Changed type hint for downtime
    """Parses command line arguments.

    Returns:
        A tuple containing the year, month, and downtime duration string.
    """
    parser = argparse.ArgumentParser(
        description='Calculate monthly uptime based on downtime duration (e.g., "90m", "1.5h", "1h 30m", "45" [defaults to minutes]).'
    )
    parser.add_argument("year", type=int, help="The year")
    parser.add_argument("month", type=int, help="The month (1-12)")
    # Changed type to str to accept flexible input
    parser.add_argument("downtime", type=str, help='Downtime duration (e.g., "90m", "1.5h", "1h 30m", "45" [minutes])')
    args = parser.parse_args()
    return args.year, args.month, args.downtime


def main():
    """Main function."""
    year, month, downtime_str = parse_arguments()

    # Parse the duration string into hours
    downtime_hours = parse_duration_to_hours(downtime_str)

    if downtime_hours is None:
        print(f"Error: Invalid downtime format: '{downtime_str}'. Use formats like '1h 30m', '90m', '1.5h', or '45' (for minutes).")
        sys.exit(1) # Exit with an error code

    # Proceed with calculation if parsing was successful
    result = calculate_uptime(year, month, downtime_hours)
    print(result)


if __name__ == "__main__":
    main()

