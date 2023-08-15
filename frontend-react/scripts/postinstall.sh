#!/usr/bin/env sh

# Move to repo root to run commands that require .git
cd ..;
husky install frontend-react/.husky;
# husky overrides the repo's hooks directory, so we set it back afterwards.
# the monorepo's precommit will call husky's precommit.
# this won't be needed once frontend is its own repo!
git config core.hooksPath ".git/hooks";

# Move back to frontend project for project commands
cd frontend-react;
patch-package;

echo "Frontend postinstall complete"