param (
    [string]$pass,
    [string]$user
)
function Get-BasicAuthCreds {
    param([string]$Username, [string]$Password)
    $AuthString = "{0}:{1}" -f $Username, $Password
    $AuthBytes = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}
$data = [pscustomobject]@{
    InactiveMBUsers = @()
}
$BasicCreds = Get-BasicAuthCreds -Username $user -Password $pass
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


$encodedslackMessageUsers = [System.Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($jsonstring))
Write-Output "SLACK_MESSAGE_Users=$encodedslackMessageUsers" >> $env:GITHUB_ENV
