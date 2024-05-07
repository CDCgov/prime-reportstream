$endpoint = "https://api.github.com/repos/CDCgov/prime-reportstream/branches"
function Get-BasicAuthCreds {
    param([string]$Username,[string]$Password)
    $AuthString = "{0}:{1}" -f $Username,$Password
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}

$BasicCreds = Get-BasicAuthCreds -Username "SupriyaAddagada" -Password ${ secrets.GITHUB_TOKEN }
$val = Invoke-WebRequest -Uri $endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
$json = $val | ConvertFrom-JSON
$limit = [datetime]::Now.AddDays(-90)

$ExcludeBranchesList = @(Get-Content .\.github\scripts\stale_items_report\excludebrancheslist.txt)

$markdownTable = @"
| Branch Name | Commit Hash | Author | Last Commit Date |
|-------------|-------------|--------|------------------|`n
"@

foreach($obj in $json)
{
    $BranchName = $obj.name
    $BasicCreds1 = Get-BasicAuthCreds -Username "SupriyaAddagada" -Password ${ secrets.GITHUB_TOKEN }
    $endpoint1 = "https://api.github.com/repos/CDCgov/prime-reportstream/branches/$BranchName"
    $Branch = Invoke-WebRequest -Uri $endpoint1 -Headers @{"Authorization"="Basic $BasicCreds1"}
    $jsonBranch = $Branch | ConvertFrom-JSON

    if($obj.commit.commit.author.date -lt $limit -and $jsonBranch.name -notin $ExcludeBranchesList)
    {
        $markdownTable += "| $($jsonBranch.name) | $($obj.commit.sha) | $($jsonBranch.commit.commit.author.name) | $($jsonBranch.commit.commit.author.date) |`n"
    }
}

Write-Host $markdownTable

$encodedMarkdownTable = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($markdownTable))
echo "STALE_BRANCHES_MARKDOWN=$encodedMarkdownTable" | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
