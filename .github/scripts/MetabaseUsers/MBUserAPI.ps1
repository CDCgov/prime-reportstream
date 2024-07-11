
$SixMonthsOld=(Get-Date).AddMonths(-6)
$stgendpoint = "https://staging.prime.cdc.gov/metabase/api/user"
$val = Invoke-RestMethod -Uri $stgendpoint -Method Get

$LastLogin = $val.last_login
$commonName=""

if ($LastLogin -gt $SixMonthsOld)
{
    $commonName = $val.common_name

}

Write-Host "Name - " $commonName
