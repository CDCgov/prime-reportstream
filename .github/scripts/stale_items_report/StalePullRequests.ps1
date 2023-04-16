# See https://developer.github.com/v3/pulls/#list-pull-requests

$endpoint = "https://api.github.com/repos/CDCgov/prime-reportstream/pulls?state=open"

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

#Write-Host $val
$root = @{ records = New-Object 'System.Collections.Generic.List[object]' }
$data = [pscustomobject]@{
    staleprs = @()
}
foreach($obj in $json)
{
    $data.staleprs += @{
        pullrequest       = $obj.number
        Title             = $obj.title
        Url               = $obj.url
        user              = $obj.user.login
    }
}
$json1 = $data | ConvertTo-Json

#Write-Host $json1
# New-Item ${runner.temp }\sample.json
# Set-Content  ${runner.temp }\sample.json $json1

#$json1 | Out-File -FilePath "${runner.temp }\sample.json"

echo "Stale_pullrequests=test"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append

#     if($obj.   -lt $limit){
#     Write-Host "Pull request: #" + $obj.number
#     Write-Host "Title: " + $obj.title
#     Write-Host "Url: " + $obj.url
    
#     # $releaseNotes = $releaseNotes + "Body: "
#     # $obj.body.Split("`n") | ForEach { 
#     #     # ignore comments from issue templates
#     #     if($_.Trim().StartsWith("<!---") -eq $FALSE)
#     #     {
#     #         $releaseNotes = $releaseNotes + $_ + "`n" 
#     #     }
#     #  }   
#     # $releaseNotes = $releaseNotes + "`n"
    
#     Write-Host "User: " + $obj.user.login
#     Write-Host ""
#     }
# }