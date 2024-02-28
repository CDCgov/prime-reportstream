StaleItemsReport.yml
**Automatically report stale branches, issues and pull requests.**

The workflow runs 1st of every month to report Pull requests, branches and Issues that do not have any activity. 
&nbsp;  
**Stale Issues:**
Github Api - https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open
script location - scripts\stale_items_report\StaleIssues.ps1
output format – Json
&nbsp; 
Reporting of Stale issues is not using any Bot, we have written code in-house and pulled the stale issues using the Api mentioned above, we can customize/extend the script to mark the issues to be closed or tagged in the future.
&nbsp; &nbsp; 
**Stale Branches:**
Github Api - https://api.github.com/repos/CDCgov/prime-reportstream/branches
Script location - scripts\stale_items_report\StaleBranches
Exclude branches - scripts\stale_items_report\excludedbrancheslist.txt
Output Format – Json
&nbsp; 
Reporting of Stale branches is not using any Bot, we have written code in-house and pulled the stale branches using the Api mentioned above, branches that can be excluded/needs to be kept permanently should be kept in excludedbrancheslist.txt. we can customize/extend the script to remove the stale branches in the future.

&nbsp; &nbsp; 
**Stale Pull requests**
Github Api - https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open
Script location - scripts\stale_items_report\StalePullRequests.ps1
Output Format – Json
&nbsp; 
Reporting of Stale pull requests is not using any Bot, we have written code in-house and pulled the stale pull requests using the Api mentioned above. we can customize/extend the script to tag/remove the stale pull requests in the future.