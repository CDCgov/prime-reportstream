param (
    [string]$PD_KEY,
    [string]$ScheduleIds
)
# $ScheduleIds = "P5ZSLDQ,P5ZSLDQ,P5DMFTU"
# $PD_KEY = ""
$Schedules = $ScheduleIds.Split(",")

$data = [pscustomobject]@{
    oncallSchedule = @()
}

foreach ($ScheduleId in $Schedules) {
    # Output the current item
    Write-Host $ScheduleId

    $endpoint = "https://api.pagerduty.com//oncalls?include[]=users&schedule_ids[]=$ScheduleId&earliest=true"
    $headers = New-Object "System.Collections.Generic.Dictionary[[String],[String]]"
    $headers.Add("Authorization", "Token token=$PD_KEY")
    $headers.Add("Accept", "application/vnd.pagerduty+json;version=2")
    $headers.Add("Content-Type", "application/json")


    $val = Invoke-RestMethod -Uri $endpoint -Headers $headers -Method Get

    $jsonstring = $val.oncalls | ConvertTo-Json -Compress -Depth 100
    $ScheduleName=$val.oncalls.schedule.summary
    # Write-Host "current on call person name - " $val.oncalls.user.name
    # Write-Host "current on call person email - " $val.oncalls.user.email
    $enddate = [DateTime]$val.oncalls.end

    $NextOncallStart = $enddate.AddDays(1).ToString('dd-MM-yyyy')
    $NextOncallend = $enddate.AddDays(7).ToString('dd-MM-yyyy')

    $Nextoncallendpoint = "https://api.pagerduty.com//oncalls?include[]=users&schedule_ids[]=$ScheduleId&earliest=true&until=$NextOncallend&since=$NextOncallStart"

    $Nextoncallval = Invoke-RestMethod -Uri $Nextoncallendpoint -Headers $headers -Method Get

    # Write-Host "Next on call person name - " ($Nextoncallval.oncalls.user.name -join ', ') 
    # Write-Host "Next on call person email - " ($Nextoncallval.oncalls.user.email -join ', ') 


    $Name = $val.oncalls.user.name
    $Until = $val.oncalls.end
    $NextPersonName = $Nextoncallval.oncalls.user.name
    $NextFrom = $Nextoncallval.oncalls.start
    $NextUntil = $Nextoncallval.oncalls.end

    $data.oncallSchedule += @{
        ScheduleName     = $ScheduleName
        OnCallPersonName = $Name
        Until            = $Until
        NextPersonName   = $NextPersonName
        NextFrom         = $NextFrom
        NextUntil        = $NextUntil
    }



    
    # echo "PersonName=$Name"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
    # echo "Until=$Until"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
    # echo "NextPersonName=$NextPersonName"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
    # echo "NextFrom=$NextFrom"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
    # echo "NextUntil=$NextUntil"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append
}
$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring
echo "PDSchedules=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append