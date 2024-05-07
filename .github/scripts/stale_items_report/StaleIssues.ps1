$endpoint = "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open"

function Get-BasicAuthCreds {
    param([string]$Username,[string]$Password)
    $AuthString = "{0}:{1}" -f $Username,$Password
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}

$BasicCreds = Get-BasicAuthCreds -Username "SupriyaAddagada" -Password ${ secrets.GITHUB_TOKEN }
$val = Invoke-WebRequest -Uri $endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
$json = $val | ConvertFrom-JSON
$limit = [datetime]::Now.AddDays(-3)

$markdownTable = @"
| Issue Number | Title | URL | User |
|--------------|-------|-----|------|`n
"@

foreach($obj in $json)
{
    if($obj.updated_at -lt $limit)
    {
        $markdownTable += "| $($obj.number) | $($obj.title) | $($obj.html_url) | $($obj.user.login) |`n"
    }
}

Write-Host $markdownTable

$encodedMarkdownTable = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($markdownTable))
echo "STALE_ISSUES_MARKDOWN=$encodedMarkdownTable" | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
