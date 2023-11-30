#!/usr/bin/env bash

APPROUTER_FILE="AppRouter.tsx"
MODIFIED_FILES=$(git diff --name-only)

# Check if the AppRouter file is in the list of modified files
if echo "$MODIFIED_FILES" | grep -q "$APPROUTER_FILE"; then
    read -r -p "You modified $APPROUTER_FILE, did you also need to update the sitemap.xml? (Y/n): " response

    # Convert to lowercase and trim whitespace
    response=$(echo "$response" | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')

    if [[ "$response" =~ ^(yes|y)$ ]]; then
        echo "Proceeding with commit."
    else
        echo "Exiting."
        exit 0
    fi
fi
