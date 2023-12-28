
$endpoint = "https://api.pagerduty.com/schedules"
function Get-BasicAuthCreds {
    param([string]$Token)
    $AuthString = "'Token token='{0}" -f $Token
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}


$BasicCreds = Get-BasicAuthCreds -Token ${ secrets.PD_ROTATION_SLACK_NOTIFICATION }
$val = Invoke-WebRequest -Uri $endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
$json = $val | ConvertFrom-JSON
Write-Host $json
# $limit = [datetime]::Now.AddDays(-90)

# #Write-Host $val
# $root = @{ records = New-Object 'System.Collections.Generic.List[object]' }
# $data = [pscustomobject]@{
#     StaleBranches = @()
# }
# $ExcludeBranchesList = @(Get-Content .\.github\scripts\stale_items_report\excludebrancheslist.txt)
# # Write-Output $json
# foreach($obj in $json)
# {
#     $BranchName =$obj.name
#     $BasicCreds1 = Get-BasicAuthCreds -Username "SupriyaAddagada" -Password ${ secrets.GITHUB_TOKEN }
#       $endpoint1 = "https://api.github.com/repos/CDCgov/prime-reportstream/branches/$BranchName"
#       #Write-Output $endpoint1
#      $Branch = Invoke-WebRequest -Uri $endpoint1 -Headers @{"Authorization"="Basic $BasicCreds1"}
#     $jsonBranch = $Branch | ConvertFrom-JSON
#     #  Write-Output $jsonBranch
#     if($obj.commit.commit.author.date -lt $limit){
#         if($jsonBranch.name -notin $ExcludeBranchesList){
#                 $data.StaleBranches += @{
#                 BranchName       = $jsonBranch.name
#                 Hash             = $obj.commit.sha
#                 Author           = $jsonBranch.commit.commit.author.name
#                 DateRelative     = $jsonBranch.commit.commit.author.date
                
#                 }
#         }

#     }
# }


# $json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring

 
echo "Stale_Branches=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append