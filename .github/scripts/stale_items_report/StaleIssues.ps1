# See https://developer.github.com/v3/pulls/#list-pull-requests

$endpoint = "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open"

function Get-BasicAuthCreds {
    param([string]$Username,[string]$Password)
    $AuthString = "{0}:{1}" -f $Username,$Password
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}

$BasicCreds = Get-BasicAuthCreds -Username "SupriyaAddagada" -Password "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"
$val = Invoke-WebRequest -Uri $endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
$json = $val | ConvertFrom-JSON
$limit = [datetime]::Now.AddDays(-90)
write-host $json
foreach($obj in $json)
{
    if($obj.lastupdated -lt $limit){
    Write-Host "Issue: #" + $obj.number
    Write-Host "Title: " + $obj.title
    Write-Host "Url: " + $obj.html_url
    
    $releaseNotes = $releaseNotes + "Body: "
    $obj.body.Split("`n") | ForEach { 
        # ignore comments from issue templates
        if($_.Trim().StartsWith("<!---") -eq $FALSE)
        {
            $releaseNotes = $releaseNotes + $_ + "`n" 
        }
     }   
    $releaseNotes = $releaseNotes + "`n"
    
    Write-Host "User: " + $obj.user.login
    Write-Host ""
    }
}