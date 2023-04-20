# See https://developer.github.com/v3/pulls/#list-pull-requests


$pages=20
function Get-BasicAuthCreds {
    param([string]$Username,[string]$Password)
    $AuthString = "{0}:{1}" -f $Username,$Password
    $AuthBytes  = [System.Text.Encoding]::Ascii.GetBytes($AuthString)
    return [Convert]::ToBase64String($AuthBytes)
}
$json=@()
for ($i=0; $i -le $pages; $i++)
{
    $endpoint = "https://api.github.com/repos/CDCgov/prime-reportstream/issues?state=open&&per_page=100&&page=$i"
    $BasicCreds = Get-BasicAuthCreds -Username ${ secrets.GITHUB_User } -Password ${ secrets.GITHUB_TOKEN }
    $val = Invoke-WebRequest -Uri $endpoint -Headers @{"Authorization"="Basic $BasicCreds"}
    $jsontest= $val | ConvertFrom-JSON
   
    $json+=$jsontest
}
$limit = [datetime]::Now.AddDays(-5)
$limitdate= $limit.ToString('yyyy-MM-dd')
$data = [pscustomobject]@{
    staleissues = @()
}

foreach($obj in $json)
{

    $updatedate= ([DateTime]($obj.created_at)).ToString('yyyy-MM-dd')
    #write-host $updatedate
    if($updatedate -lt $limitdate){

    $data.staleissues += @{
        Issue       = $obj.number
        Title             = $obj.title
        Url               = $obj.html_url
        user              = $obj.user.login
    }

    }
}
$json1 = $data | ConvertTo-Json

$jsonstring=$json1 | ConvertFrom-Json | ConvertTo-Json -Compress -Depth 100
 Write-Host $jsonstring
echo "Stale_Issues=$jsonstring"  | Out-File -FilePath $Env:GITHUB_ENV -Encoding utf8 -Append

