
$file = "$GITHUB_WORKSPACE\sample.json"
$text = "Automated edit"
$wi = "#13 #14"

"Set config"
git config --global user.email "supriya.addagada@icf.com" # any values will do, if missing commit will fail
git config --global user.name "supriya addagada"

$currentBranch=`git rev-parse --symbolic-full-name --abbrev-ref HEAD`

"Select a branch"
git checkout $currentBranch 2>&1 | write-host # need the stderr redirect as some git command line send none error output here

"Update the local repo"
git pull  2>&1 | write-host

"Status at start"
git status 2>&1 | write-host

"Update the file $file"
Add-Content -Path $file -Value "$text - $(Get-Date)"

"Status prior to stage"
git status 2>&1 | write-host

"Stage the file"
git add $file  2>&1 | write-host

"Status prior to commit"
git status 2>&1 | write-host

"Commit the file"
git commit -m "Automated Repo Update $wi"  2>&1 | write-host

"Status prior to push"
git status 2>&1 | write-host

"Push the change"
git push  2>&1 | write-host