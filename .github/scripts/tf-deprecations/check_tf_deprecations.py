#!/usr/bin/env python3
import json
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
        print("Usage: python parse_deprecations.py <plan.json>")
        sys.exit(1)
    
    plan_file = sys.argv[1]
    try:
        with open(plan_file, 'r') as f:
            plan = json.load(f)
    except Exception as e:
        print(f"Error reading {plan_file}: {e}")
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
