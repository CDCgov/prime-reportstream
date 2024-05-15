function Get-BasicAuthCreds {
    param([string]$Username,[string]$Password)
    $AuthString = "{0}:{1}" -f $Username,$Password
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}

function Get-StaleItems {
    param(
        [string]$ItemType,
        [string]$Endpoint,
        [int]$StaleDays
    )

    $BasicCreds = Get-BasicAuthCreds -Username "repo" -Password ${ secrets.GITHUB_TOKEN }
    $val = Invoke-WebRequest -Uri $Endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
    $json = $val | ConvertFrom-JSON
    $limit = [datetime]::Now.AddDays(-$StaleDays)

    $staleItems = $json | Where-Object { $_.updated_at -lt $limit }

    return $staleItems
}

# Get stale pull requests
$stalePullRequests = Get-StaleItems -ItemType "Pull Request" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/pulls?direction=asc&sort=updated&state=open" -StaleDays 90
$stalePullRequestCount = $stalePullRequests.Count

# Get stale branches
$ExcludeBranchesList = @(Get-Content .\.github\scripts\stale_items_report\excludebrancheslist.txt)
$staleBranches = Get-StaleItems -ItemType "Branch" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/branches?direction=asc&sort=updated" -StaleDays 90 | Where-Object { $_.name -notin $ExcludeBranchesList }
$staleBranchCount = $staleBranches.Count

# Get stale issues
$staleIssues = Get-StaleItems -ItemType "Issue" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open&per_page=100&direction=asc&sort=updated" -StaleDays 90
$staleIssueCount = $staleIssues.Count

# Count issues older than 90 days for each issue author
$issueAuthorCounts = $staleIssues | Group-Object -Property user.login | Select-Object Name, Count

$slackMessage = @"
*Stale GitHub Items Summary*

- Stale Pull Requests (>90 days): $stalePullRequestCount
- Stale Branches (>90 days): $staleBranchCount
- Stale Issues (>90 days): $staleIssueCount

*Stale Issues by Author*
$($issueAuthorCounts | ForEach-Object { "- $($_.Name): $($_.Count)" })
"@

$encodedSlackMessage = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($slackMessage))
Write-Output "SLACK_MESSAGE=$encodedSlackMessage" >> $env:GITHUB_ENV
