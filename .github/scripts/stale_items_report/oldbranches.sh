#!/usr/bin/bash

# --------------------------- FUNCTIONS ---------------------------------
_line() {
	printf %80s |tr " " "-"; echo ""
}

_info() {
	echo -e 1>&2 "\033[32m"$@"\033[0m"
}

_heading() {
	echo ""
	echo -e 1>&2 "\033[34m--> "$@"\033[0m"
	_line
}

#ExcludeBranchesinput="./.github/scripts/stale_items_report/excludebrancheslist.txt"

# ----------------------------------------------------------------------
# 1. Find the currently checked out branch
currentBranch=`git rev-parse --symbolic-full-name --abbrev-ref HEAD`

# 2. Prune branches which are deleted on origin
_heading "Runs git fetch --prune"
git fetch --prune
_info "done :)"

# 3. List branches that are merged to the current branch
_heading "Branches that are MERGED to $currentBranch:"

git branch -r --merged | while read branch
do
    
	if [ "$branch" != "origin/HEAD -> origin/master" ]  && [ "$branch" != "origin/master" ];
	then
        # while IFS= read -r line
        # do
            # if [ "$branch" != $line ];
            # then
                echo $branch
                branchSHA=`git rev-parse $branch`
                branchLastUpdate=`git show -s --format="%ci" $branchSHA`
                echo $branchLastUpdate
        
                timeago='90 days ago'

                lastupdateSec=$(date --date "$branchLastUpdate" +'%s')    # For "now", use $(date +'%s')
                taSec=$(date --date "$timeago" +'%s')

                # echo "INFO: dtSec=$lastupdateSec, taSec=$taSec" >&2

                if [ "$lastupdateSec" -lt "$taSec" ];
                then
                name=`git log --pretty=format:"%Cred%an%Creset" -1 $branch`
                time=`git log --pretty=format:"%Cgreen%ci %Cblue%cr%Creset" -1 $branch`
                echo -e $time $name $branch
                fi
            # fi
        # done < "$ExcludeBranchesinput"
        
	fi
done | sort

# 4. List branches that are not merged to the current branch
_heading "Branches that are NOT MERGED to $currentBranch:"

git branch -r --no-merged | while read branch
do
	if [ "$branch" != "origin/HEAD -> origin/master" ] && [ "$branch" != "origin/master" ];
	then
        #  while IFS= read -r line
        # do
            if [ "$branch" != $line ];
            then
                echo $branch
                branchSHA=`git rev-parse $branch`
                branchLastUpdate=`git show -s --format="%ci" $branchSHA`
                echo $branchLastUpdate
        
                timeago='90 days ago'

                lastupdateSec=$(date --date "$branchLastUpdate" +'%s')    # For "now", use $(date +'%s')
                taSec=$(date --date "$timeago" +'%s')

                # echo "INFO: dtSec=$lastupdateSec, taSec=$taSec" >&2

                if [ "$lastupdateSec" -lt "$taSec" ];
                then
                    name=`git log --pretty=format:"%Cred%an%Creset" -1 $branch`
                    time=`git log --pretty=format:"%Cgreen%ci %Cblue%cr%Creset" -1 $branch`	
                    numberOfCommitsNotMerged=`git log $branch  ^$currentBranch --no-merges --pretty=format:"%h - %an, %ar : %s" | wc -l`
                    numberOfCommitsNotMerged=$(($numberOfCommitsNotMerged + 1)) # Have to add 1, don't ask why... Magic number FTW...
                    echo -e $time $name $branch '\033[33m'$numberOfCommitsNotMerged' umerged commits\033[0m'
                     fi
            fi
        # done < "$ExcludeBranchesinput"
	fi
done | sort


# 5. Provide some help on how to check what's not merged, and how to delete (to spare people of that trip to google...)
_heading "A little help to get you going:"
_info "Run this to see all the commits that are not merged to this branch in another branch:"
echo "git log origin/<branchYouWantToDiff> ^$currentBranch --no-merges"
echo ""
_info "Run this to delete a branch on origin:"
echo "git push origin :<branchToDelete>"