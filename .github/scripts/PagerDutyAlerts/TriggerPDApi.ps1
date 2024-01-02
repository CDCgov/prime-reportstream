
$endpoint = "https://api.pagerduty.com/schedules?Accept=application/vnd.pagerduty+json;version=2&Content-Type=application/json"

# $headers = @{
#     'Authorization' = "Token token=${ secrets.PD_ROTATION_SLACK_NOTIFICATION }"
#     'Accept'        = 'application/vnd.pagerduty+json;version=2'
#     'Content-Type'  = 'application/json'
# }

$headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
$headers.Add("Authorization","Token token=${ secrets.PD_ROTATION_SLACK_NOTIFICATION }")
# $headers.Add("Accept", "application/vnd.pagerduty+json;version=2")
# $headers.Add("Content-Type", "application/json")

# $BasicCreds = Get-BasicAuthCreds -Token ${ secrets.PD_ROTATION_SLACK_NOTIFICATION }

$val = Invoke-RestMethod -Uri $endpoint -Headers $headers -Method Get



$json = $val | ConvertFrom-JSON
Write-Host $json
# $limit = [datetime]::Now.AddDays(-90)
