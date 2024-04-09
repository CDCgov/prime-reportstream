#!/usr/bin/env bash

RC=0

echo "Checking for frontend changes..."

if [[ -z $(git diff --name-only --cached frontend-react/) ]]; then
    echo "git diff is reporting there are no uncommitted changes within 'frontend-react'; hence, skipping frontend checks."
else
  cd frontend-react

  # make sure frontend environment was set up first

  # skip if in CI mode or husky explicitly disabled
  if [[ -z "$CI" && (-z "$HUSKY" || "$HUSKY" != "0") && ! -d ".husky/_" ]]; then
    echo "frontend> Husky not found"
    RC=1
  fi

  if [[ ! -d "node_modules" ]]; then
    echo "frontend> Node modules not found"
    RC=1
  fi

  if [[ ${RC?} != 0 ]]; then
      echo "frontend> ERROR: Your frontend environment is not set up. Please run the 'yarn' command inside the 'frontend-react' folder."
  else
    # Call what would normally be called for frontend if this wasn't a monorepo
    .husky/_/pre-commit

    RC=$?
    if [[ ${RC?} != 0 ]]; then
        echo "frontend> ERROR: You likely have frontend violations"
    fi

    echo "frontend> Done."
  fi
fi

exit ${RC?}