param (
    [string]$PD_KEY
)

$endpoint = "https://api.pagerduty.com//oncalls?include[]=users&schedule_ids[]=PE8NLRU&earliest=true"
$headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
$headers.Add("Authorization","Token token=$PD_KEY")
$headers.Add("Accept", "application/vnd.pagerduty+json;version=2")
$headers.Add("Content-Type", "application/json")


$val = Invoke-RestMethod -Uri $endpoint -Headers $headers -Method Get

$jsonstring=$val.oncalls | ConvertTo-Json -Compress -Depth 100
Write-Host "current on call person name - " $val.oncalls.user.name
Write-Host "current on call person email - " $val.oncalls.user.email
$enddate = [DateTime]$val.oncalls.end

$NextOncallStart=$enddate.AddDays(1).ToString('dd-MM-yyyy')
$NextOncallend=$enddate.AddDays(7).ToString('dd-MM-yyyy')

$Nextoncallendpoint = "https://api.pagerduty.com//oncalls?include[]=users&schedule_ids[]=PE8NLRU&earliest=true&until=$NextOncallend&since=$NextOncallStart"

$Nextoncallval = Invoke-RestMethod -Uri $Nextoncallendpoint -Headers $headers -Method Get

Write-Host "Next on call person name - " ($Nextoncallval.oncalls.user.name -join ', ') 
Write-Host "Next on call person email - " ($Nextoncallval.oncalls.user.email -join ', ') 

$data = [pscustomobject]@{
    OnCallDetails = @()
}

$data.OnCallDetails += @{
PersonName       = $val.oncalls.user.name
PersonEmail      = $val.oncalls.user.email
NextPersonName   = $Nextoncallval.oncalls.user.name
user             = $Nextoncallval.oncalls.user.email
}

$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring

 $Object = [String]::Join(",",(ConvertFrom-Json $json1))


echo "OnCallPerson=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
echo "OnCallDetails=$Object"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append