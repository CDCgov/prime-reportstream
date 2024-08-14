param (
    [string]$pass
)

function Get-BasicAuthCreds {
    param([string]$Username, [string]$Password)
    $AuthString = "{0}:{1}" -f $Username, $Password
    $AuthBytes = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}

function Get-StaleItems {
    param(
        [string]$ItemType,
        [string]$Endpoint,
        [int]$StaleDays
    )

    $BasicCreds = Get-BasicAuthCreds -Username "repo" -Password $pass
    $headers = @{"Authorization" = "Basic $BasicCreds"}
    $limit = [datetime]::Now.AddDays(-$StaleDays)

    $json = Invoke-WebRequest -Uri $Endpoint -Headers $headers | ConvertFrom-JSON

    $staleItems = switch ($ItemType) {
        "Issue" {
            $json | Where-Object { [datetime]::Parse($_.updated_at) -lt $limit }
        }
        "Branch" {
            $json | ForEach-Object {
                $branchJson = Invoke-WebRequest -Uri $_.commit.url -Headers $headers | ConvertFrom-JSON
                $updatedAt = [datetime]::Parse($branchJson.commit.author.date)
                if ($updatedAt -lt $limit -and $branchJson.author.login) {
                    $branchJson
                }
            }
        }
        "PullRequest" {
            $json | Where-Object { [datetime]::Parse($_.updated_at) -lt $limit }
        }
        default {
            Write-Error "Invalid item type: $ItemType"
            return
        }
    }

    return $staleItems
}

function Get-AllStaleItems {
    param(
        [string]$ItemType,
        [string]$Endpoint,
        [int]$StaleDays
    )

    $staleItems = @()
    $page = 1
    $perPage = 100

    do {
        $url = "$Endpoint&page=$page&per_page=$perPage"
        $items = Get-StaleItems -ItemType $ItemType -Endpoint $url -StaleDays $StaleDays
        $staleItems += $items
        $page++
    } while ($items.Count -eq $perPage)

    return $staleItems
}

function Format-AuthorCounts {
    param(
        [object[]]$Items,
        [string]$ItemType,
        [int]$TopAuthors,
        [string]$AuthorProperty,
        [switch]$IncludeLink
    )

    $authorCounts = $Items |
        Select-Object @{Name = 'UserLogin'; Expression = { $_.$AuthorProperty.login.replace('[bot]', '') } } |
        Group-Object -Property UserLogin -NoElement |
        Sort-Object Count -Descending |
        Select-Object -First $TopAuthors

    $authorCountsFormatted = $authorCounts | ForEach-Object {
        $authorLogin = $_.Name
        if ($IncludeLink) {
            $itemTypeUrl = if ($ItemType -eq "PullRequest") { "pulls" } else { $ItemType.ToLower() + "s" }
            $authorItemsLink = "https://github.com/CDCgov/prime-reportstream/${itemTypeUrl}?q=is:open+sort:created-asc+author:$authorLogin"
            "- [$authorLogin]($authorItemsLink): $($_.Count)"
        } else {
            "- ${authorLogin}: $($_.Count)"
        }
    }

    return $authorCountsFormatted
}

function Get-StaleItemCounts {
    param(
        [string]$ItemType,
        [string]$Endpoint,
        [int]$StaleDays,
        [int]$TopAuthors,
        [string]$AuthorProperty,
        [switch]$IncludeLink
    )

    $staleItems = Get-AllStaleItems -ItemType $ItemType -Endpoint $Endpoint -StaleDays $StaleDays
    $staleItemsFiltered = if ($ItemType -eq "Issue") { $staleItems | Where-Object { $_.html_url -notmatch "/pull/" } } else { $staleItems }
    $authorCountsFormatted = Format-AuthorCounts -Items $staleItemsFiltered -ItemType $ItemType -TopAuthors $TopAuthors -AuthorProperty $AuthorProperty -IncludeLink:$IncludeLink

    return @{
        Count = $staleItemsFiltered.Count
        AuthorCounts = $authorCountsFormatted
    }
}


function Get-SlackMessage {
    param(
        [string]$Title,
        [int]$Count,
        [string[]]$AuthorCounts
    )

    return @"
*$Title*
---
$($AuthorCounts -join "`n")

"@
}

# Configuration
$topPullRequestAuthors = 5
$stalePullRequestDays = 180
$topBranchAuthors = 5
$staleBranchDays = 180
$topIssueAuthors = 15
$staleIssueDays = 180

# Get stale item counts and author counts
$stalePullRequests = Get-StaleItemCounts -ItemType "PullRequest" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/pulls?direction=asc&sort=updated&state=open" -StaleDays $stalePullRequestDays -TopAuthors $topPullRequestAuthors -AuthorProperty "user" -IncludeLink
$staleBranches = Get-StaleItemCounts -ItemType "Branch" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/branches?direction=asc&sort=updated&protected=false" -StaleDays $staleBranchDays -TopAuthors $topBranchAuthors -AuthorProperty "author"
$staleIssues = Get-StaleItemCounts -ItemType "Issue" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open&direction=asc&sort=updated" -StaleDays $staleIssueDays -TopAuthors $topIssueAuthors -AuthorProperty "user" -IncludeLink

# Prepare Slack messages
$slackMessageSummary = @"

- Stale Pull Requests (>$stalePullRequestDays days): $($stalePullRequests.Count)
- Stale Branches (>$staleBranchDays days): $($staleBranches.Count)
- Stale Issues (>$staleIssueDays days): $($staleIssues.Count)

"@

$slackMessagePullRequests = Get-SlackMessage -Title "Stale Pull Requests by Author (Top $topPullRequestAuthors)" -Count $stalePullRequests.Count -AuthorCounts $stalePullRequests.AuthorCounts
$slackMessageBranches = Get-SlackMessage -Title "Stale Branches by Author (Top $topBranchAuthors)" -Count $staleBranches.Count -AuthorCounts $staleBranches.AuthorCounts
$slackMessageIssues = Get-SlackMessage -Title "Stale Issues by Author (Top $topIssueAuthors)" -Count $staleIssues.Count -AuthorCounts $staleIssues.AuthorCounts

# Output Slack messages
Write-Host $slackMessageSummary
Write-Host $slackMessagePullRequests
Write-Host $slackMessageBranches
Write-Host $slackMessageIssues

# Set GitHub environment variables
$encodedslackMessageSummary = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($slackMessageSummary))
$encodedslackMessagePullRequests = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($slackMessagePullRequests))
$encodedslackMessageBranches = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($slackMessageBranches))
$encodedslackMessageIssues = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($slackMessageIssues))
Write-Output "SLACK_MESSAGE_SUMMARY=$encodedslackMessageSummary" >> $env:GITHUB_ENV
Write-Output "SLACK_MESSAGE_PULL_REQUESTS=$encodedslackMessagePullRequests" >> $env:GITHUB_ENV
Write-Output "SLACK_MESSAGE_BRANCHES=$encodedslackMessageBranches" >> $env:GITHUB_ENV
Write-Output "SLACK_MESSAGE_ISSUES=$encodedslackMessageIssues" >> $env:GITHUB_ENV
