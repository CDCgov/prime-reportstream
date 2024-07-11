param (
    [string]$pass
)
param (
    [string]$user
)

function Get-BasicAuthCreds {
    param([string]$Username, [string]$Password)
    $AuthString = "{0}:{1}" -f $Username, $Password
    $AuthBytes = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}
$BasicCreds = Get-BasicAuthCreds -Username $user -Password $pass
$headers = @{"Authorization" = "Basic $BasicCreds"}
$SixMonthsOld=(Get-Date).AddMonths(-6)
$stgendpoint = "https://staging.prime.cdc.gov/metabase/api/user"
$val = Invoke-RestMethod -Uri $stgendpoint  -Headers $headers -Method Get

$LastLogin = $val.last_login
$commonName=""

if ($LastLogin -gt $SixMonthsOld)
{
    $commonName = $val.common_name

}

Write-Host "Name - " $commonName
