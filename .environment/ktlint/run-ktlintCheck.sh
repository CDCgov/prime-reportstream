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


# This gets the list of staged files that will be included in the commit (removed files are ignored).
# If a file is only partially staged (some changes in it are unstaged), then the file is not included
# in checks. That is because the checks re-add the file to ensure autocorrected fixes are included
# in the commit, and we don't want to add the whole file since that would mess with the intention
# of partial staging.
stagedFilename=.tmpStagedFiles
unstagedFilename=.tmpUnstagedFiles

git diff --cached --name-only --diff-filter=ACM | grep '\.kt[s"]\?$' | sort > $stagedFilename
git diff --name-only --diff-filter=ACM | grep '\.kt[s"]\?$' | sort > $unstagedFilename

# This returns a list of lines that are only in the staged list
filesToCheck=$(comm -23 $stagedFilename $unstagedFilename | tr '\n' ' ')

# Delete the files, which were created for temporary processing
rm -f $stagedFilename
rm -f $unstagedFilename

# If the list of files is not empty then run checks on them
if [ ! -z "$filesToCheck" ]
then
	ktlint --check $filesToCheck
	if [ $? -ne 0 ]; then exit 1; fi
	# If we haven't exited because of an error it means we were able to
	# fix issues with autocorrect. However, we need to add the changes in
	# order for them to be included in the pending commit.
	git add $filesToCheck
fi