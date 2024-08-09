# param (
#     [string]$key
# )
$key = 'mb_h7RFQv6J2zsrmlDkyE08aNWB/YPKirsK1OPO4DqCBNQ='
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

$headers = @{"x-api-key" = $key }
$SixMonthsOld = (Get-Date).AddMonths(-6)
$stgendpoint = "https://prime.cdc.gov//metabase/api/user"
$val = Invoke-RestMethod -Uri $stgendpoint  -Headers $headers -Method Get

$LastLogin = $val.last_login
$commonName = ""

foreach ($User in $val.data) {
    $LastLogin = $User.last_login
    if ($LastLogin -eq $null -Or $LastLogin -le $SixMonthsOld) {
        $commonName = $User.common_name
        Write-Host "Name - " $commonName
        Write-Host "Login - " $User.date_joined
        $MBUsers += "`n"
        if ($LastLogin) {
            $LastLoginDate = [datetime]::Parse($LastLogin)
        }
        $JoinDate = [datetime]::Parse($User.date_joined)

        $MBUsers += 'Name-' + $commonName + ' Last Login Date - ' + $LastLoginDate + ' Date Joined - ' + $JoinDate

        $data.InactiveMBUsers += @{
            Name       = $commonName
            DateJoined = $User.date_joined
            LastLogin  = $LastLogin
        }
    }
}
$json1 = $data | ConvertTo-Json
$slackMessageMBUsers = $MBUsers
Write-Host $MBUsers
$encodedslackMessageSummary = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($MBUsers))
Write-Output "SLACK_MESSAGE_SUMMARY=$encodedslackMessageSummary" >> $env:GITHUB_ENV
