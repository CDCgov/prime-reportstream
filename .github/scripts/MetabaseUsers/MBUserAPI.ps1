
$data = [pscustomobject]@{
    InactiveMBUsers = @()
}
$global:MBUsers = "Inactive Metabase users"
function Get-SlackMessage {
    param(
        [string]$Title,
        [string[]]$Name,
        [string[]]$Date
        
    )

    return @"
*$Title*
---
$Name[0]

"@
}
$headers = @{"x-api-key" = "mb_5ovOOr1U+zZ1a/MmRIB5ITEJyPTiGKh4FfV+8Bthf2w=" }
$SixMonthsOld = (Get-Date).AddMonths(-6)
$stgendpoint = "https://staging.prime.cdc.gov//metabase/api/user"
$val = Invoke-RestMethod -Uri $stgendpoint  -Headers $headers -Method Get

$LastLogin = $val.last_login
$commonName = ""

foreach ($User in $val.data) {
    $LastLogin = $User.last_login
    if ($LastLogin -eq $null  -Or $LastLogin -le $SixMonthsOld) {
            $commonName = $User.common_name
            Write-Host "Name - " $commonName
            Write-Host "Login - " $User.date_joined
            $MBUsers+="`n"
            $MBUsers +=$commonName + ' ' +$user.date_joined

            $data.InactiveMBUsers += @{
                Name       = $commonName
                DateJoined = $User.date_joined
                LastLogin  = $LastLogin
            }
    }
}
$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring
 echo "InactiveMBUsers=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append

#$slackMessageMBUsers = Get-SlackMessage -Title "Inactive Metabase Users" -Name $data.InactiveMBUsers.Name -Date $data.InactiveMBUsers.DateJoined
# $slackMessageMBUsers =$MBUsers
# Write-Host $MBUsers
#  $encodedslackMessageSummary = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($MBUsers))
#  Write-Output "SLACK_MESSAGE_SUMMARY=$encodedslackMessageSummary" >> $env:GITHUB_ENV
