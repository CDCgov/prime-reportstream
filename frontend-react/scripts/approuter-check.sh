#!/usr/bin/env bash

APPROUTER_FILE="AppRouter.tsx"
MODIFIED_FILES=$(git diff --cached --name-only)
BG_BLACK="\033[40m"
FG_WHITE="\033[97m"
RESET="\033[0m"
PROMPT="${BG_BLACK}${FG_WHITE}You modified $APPROUTER_FILE -- do you want to exit and update the sitemap.xml? (Y/n): ${RESET}"

# Check if the AppRouter file is in the list of modified files
if echo "$MODIFIED_FILES" | grep -q "$APPROUTER_FILE"; then
    # Print the prompt with colors and use \c at the end of the echo command prevents a newline
    echo -e "${BG_BLACK}${FG_WHITE}You modified $APPROUTER_FILE -- do you want to exit and update the sitemap.xml? (Y/n):${RESET}\c"
    read -r response < /dev/tty

    # Convert to lowercase and trim whitespace
    response=$(echo "$response" | tr -d '[:space:]' | tr '[:upper:]' '[:lower:]')

    if [[ "$response" =~ ^(yes|y)$ ]]; then
        echo "Exiting."
        exit 1
    else
        echo "Proceeding with commit."
    fi
fi
