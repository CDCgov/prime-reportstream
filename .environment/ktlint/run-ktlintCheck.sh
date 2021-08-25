#!/usr/bin/env bash


echo "checking for ktlint code violations..."

REPO_ROOT=$(git rev-parse --show-toplevel)
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
	$(cd ${REPO_ROOT}/prime-router/ && ktlint --check $filesToCheck)
	if [ $? -ne 0 ]; then
    error
    exit 1 
    fi
	# If no code violations are found, we add the changes
	git add $filesToCheck
fi

exit ${RC}