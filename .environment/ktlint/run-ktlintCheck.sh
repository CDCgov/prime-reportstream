#!/usr/bin/env bash

# REPO_ROOT="$(git rev-parse --show-toplevel)"
# RC=0


# echo Checking format...
# $(cd ${REPO_ROOT}/prime-router/ && ./gradlew ktlintCheck >/dev/null 2>&1)
# RC=$?
# echo Checking format finished.


# if [[ RC -ne 0 ]]; then
#     echo "ktlint found format violations!"
#     echo $erromessage 
# fi

# exit ${RC}

echo "checking for ktlint code violations..."
# This gets the list of staged files that will be included in the commit (removed files are ignored).
# If a file is only partially staged (some changes in it are unstaged), then the file is not included
# in checks. That is because the checks re-add the file to ensure autocorrected fixes are included
# in the commit, and we don't want to add the whole file since that would mess with the intention
# of partial staging.
stagedFile=.tmpStagedFiles
unstagedFile=.tmpUnstagedFiles
RC=0

git diff --cached --name-only --diff-filter=ACM | grep '\.kt[s"]\?$' | sort > $stagedFile
git diff --name-only --diff-filter=ACM | grep '\.kt[s"]\?$' | sort > $unstagedFile

function error() {
    echo "Gitleaks> ERROR: Your code has ktlint format violations!"
}

# This returns a list of lines that are only in the staged list
filesToCheck=$(comm -23 $stagedFile $unstagedFile | tr '\n' ' ')

# Delete temp files
rm -f $stagedFile
rm -f $unstagedFile

# Run ktlintCheck
if [ ! -z "$filesToCheck" ]
then
	ktlint --check $filesToCheck
	if [ $? -ne 0 ]; then
    error
    exit 1 
    fi
	# If no code violations are found, we add the changes
	git add $filesToCheck
fi

exit ${RC}