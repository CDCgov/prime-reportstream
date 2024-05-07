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
        [int]$StaleDays,
        [string[]]$ExcludeList
    )

    $BasicCreds = Get-BasicAuthCreds -Username "repo" -Password ${ secrets.GITHUB_TOKEN }
    $val = Invoke-WebRequest -Uri $Endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
    $json = $val | ConvertFrom-JSON
    $limit = [datetime]::Now.AddDays(-$StaleDays)

    $bulletPointList = ""

    foreach($obj in $json)
    {
        if($obj.updated_at -lt $limit)
        {
            $title = $obj.title -replace '`', ''

            if($ItemType -eq "Branch" -and $obj.name -notin $ExcludeList)
            {
                $BranchName = $obj.name
                $BasicCreds1 = Get-BasicAuthCreds -Username "repo" -Password ${ secrets.GITHUB_TOKEN }
                $endpoint1 = "https://api.github.com/repos/CDCgov/prime-reportstream/branches/$BranchName"
                $Branch = Invoke-WebRequest -Uri $endpoint1 -Headers @{"Authorization"="Basic $BasicCreds1"}
                $jsonBranch = $Branch | ConvertFrom-JSON
            
                if($obj.commit.commit.author.date -lt $limit)
                {
                    $bulletPointList += "- [" + $jsonBranch.name.SubString(0,[math]::min(10,$jsonBranch.name.length)) + "..." + $jsonBranch.name.Substring([math]::max(0,$jsonBranch.name.Length - 10)) + "]($($jsonBranch._links.html)): | $($jsonBranch.commit.commit.author.name)`n"
                }
            }
            elseif($ItemType -ne "Branch")
            {
                $bulletPointList += "- [#$($obj.number)]($($obj.html_url)): " + $title.SubString(0,[math]::min(10,$title.length)) + "..." + $title.Substring([math]::max(0,$title.Length - 10)) + " | $($obj.user.login)`n"
            }
        }
    }

    Write-Host $bulletPointList

    $encodedBulletPointList = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($bulletPointList))
    Write-Output "STALE_$($ItemType.ToUpper().Replace(" ", "_"))_LIST=$encodedBulletPointList" >> $env:GITHUB_ENV
}

# Stale Pull Requests
Get-StaleItems -ItemType "Pull Request" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/pulls?direction=asc&sort=updated&state=open" -StaleDays 90

# Stale Branches
$ExcludeBranchesList = @(Get-Content .\.github\scripts\stale_items_report\excludebrancheslist.txt)
Get-StaleItems -ItemType "Branch" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/branches?direction=asc&sort=updated" -StaleDays 90 -ExcludeList $ExcludeBranchesList

# Stale Issues
Get-StaleItems -ItemType "Issue" -Endpoint "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open&per_page=100&direction=asc&sort=updated" -StaleDays 90
