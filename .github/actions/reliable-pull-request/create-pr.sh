#!/bin/bash

# Create Pull Request and capture the output
PR_OUTPUT=$(gh pr create \
  --title "$INPUT_TITLE" \
  --body "$INPUT_BODY" \
  --base "$INPUT_TARGETBRANCH" \
  --head "$INPUT_SOURCEBRANCH" \
  --label "$INPUT_LABELS" \
  --assignee "$INPUT_ASSIGNEES" 2>&1)

# Extract PR URL from the output
PR_URL=$(echo "$PR_OUTPUT" | grep -o 'https://github.com/[^ ]*')

# Set the PR URL as the output
echo "PR_URL=$PR_URL" >> $GITHUB_OUTPUT
