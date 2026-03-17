#!/usr/bin/env python3
import json
import os
import sys

def find_deprecations(plan):
    """Search for diagnostics that mention deprecation warnings."""
    deprecations = []

    # The plan JSON may include a "diagnostics" field with warnings and errors.
    diagnostics = plan.get("diagnostics", [])
    for diag in diagnostics:
        # Combine summary and detail for easier matching.
        summary = diag.get("summary", "")
        detail = diag.get("detail", "")
        message = f"{summary} {detail}".strip()

        # Check if the message contains "deprecated" (case insensitive).
        if "deprecated" in message.lower():
            # Some diagnostics include an "address" key identifying the resource.
            address = diag.get("address", "unknown")
            deprecations.append({
                "address": address,
                "message": message
            })

    return deprecations

def main():
    if len(sys.argv) != 2:
        script_name = os.path.basename(sys.argv[0])
        print(f"Usage: python {script_name} <plan.json>")
        sys.exit(1)
    
    plan_file_input = sys.argv[1]
    abs_plan_file = "" # Initialize to ensure it's defined for error messages

    try:
        abs_plan_file = os.path.realpath(plan_file_input)
        allowed_base_dir = os.path.realpath(os.getcwd())

        if os.path.commonpath([abs_plan_file, allowed_base_dir]) != allowed_base_dir:
            print(f"Error: Path traversal attempt. Input file '{plan_file_input}' resolves to '{abs_plan_file}', which is outside the allowed directory '{allowed_base_dir}'.")
            sys.exit(1)
        
        if not os.path.isfile(abs_plan_file):
            print(f"Error: The path '{abs_plan_file}' is not a file or does not exist.")
            sys.exit(1)

        with open(abs_plan_file, 'r') as f:
            plan = json.load(f)
    except FileNotFoundError: # Should be caught by os.path.isfile, but good for explicit error
        print(f"Error: File not found at '{abs_plan_file}'.")
        sys.exit(1)
    except json.JSONDecodeError:
        print(f"Error: Could not decode JSON from '{abs_plan_file}'. Malformed JSON.")
        sys.exit(1)
    except Exception as e:
        # Catch other errors related to path validation or file operations
        print(f"Error processing file '{plan_file_input}' (resolved to '{abs_plan_file}'): {e}")
        sys.exit(1)

    deprecations = find_deprecations(plan)

    if deprecations:
        print("Found deprecation warnings for the following resources:\n")
        for dep in deprecations:
            print(f"Resource: {dep['address']}")
            print(f"Message: {dep['message']}\n")
    else:
        print("No deprecation warnings found in the plan.")

if __name__ == '__main__':
    main()
