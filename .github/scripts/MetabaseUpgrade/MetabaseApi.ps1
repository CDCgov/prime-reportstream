$data = [pscustomobject]@{
    UpgradeDetails = @()
}

$stgendpoint = "https://staging.prime.cdc.gov/metabase/api/session/properties"
$val = Invoke-RestMethod -Uri $stgendpoint -Method Get

$stgVersion = $val.version.tag

Write-Host "Version - " $stgVersion


$Prdendpoint = "https://prime.cdc.gov/metabase/api/session/properties"
$prdval = Invoke-RestMethod -Uri $Prdendpoint -Method Get

$PrdVersion = $prdval.version.tag

Write-Host "Version - " $PrdVersion
$upgradenecessary = 'false'
if ($stgVersion -ne $PrdVersion)
{
    $upgradenecessary = 'true'

}


$data.UpgradeDetails += @{
    StgVersion     = $stgVersion
    PrdVersion = $PrdVersion
    UpgradeRequired  = $upgradenecessary
}

$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring
echo "Versionupdates=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append

