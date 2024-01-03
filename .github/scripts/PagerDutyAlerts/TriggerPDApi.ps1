param (
    [string]$PD_KEY
)
$endpoint = "https://api.pagerduty.com//oncalls?include[]=users&schedule_ids[]=PE8NLRU&earliest=true"
$headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
$headers.Add("Authorization","Token token=$PD_KEY")
$headers.Add("Accept", "application/vnd.pagerduty+json;version=2")
$headers.Add("Content-Type", "application/json")

# $BasicCreds = Get-BasicAuthCreds -Token ${ secrets.PD_ROTATION_SLACK_NOTIFICATION }

$val = Invoke-RestMethod -Uri $endpoint -Headers $headers -Method Get

$jsonstring=$val.oncalls | ConvertTo-Json -Compress -Depth 100
Write-Host "current on call person name" $jsonstring.user.name
Write-Host "current on call person email" $jsonstring.user.email
# $limit = [datetime]::Now.AddDays(-90)
