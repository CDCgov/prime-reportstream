# See https://developer.github.com/v3/pulls/#list-pull-requests

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
$limit = [datetime]::Now.AddDays(-90)
write-host $json
$data = [pscustomobject]@{
    staleissues = @()
}

foreach($obj in $json)
{

    if($obj.lastupdated -lt $limit){

    $data.staleissues += @{
        Issue       = $obj.number
        Title             = $obj.title
        Url               = $obj.html_url
        user              = $obj.user.login
    }

    }
}
$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
# Write-Host $jsonstring
echo "Stale_Issues=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append

