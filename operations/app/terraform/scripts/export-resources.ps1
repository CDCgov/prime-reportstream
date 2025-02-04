#!/usr/bin/env pwsh

<#
.SYNOPSIS
    Fetches a list of Azure resources and exports them to a CSV file.

.DESCRIPTION
    This script retrieves Azure resources using the Azure CLI and formats the output as a CSV file.
    It extracts specified attributes like location, name, and resource group.

.PARAMETER OutputFile
    The name of the output CSV file. Default is 'azure-resources.csv'.

.PARAMETER OutputHeaders
    Specifies which properties to extract from the Azure resources.
    Default is `"Location":location,"Name":name,"Resource Group":resourceGroup`.

.EXAMPLE
    ./export-azure-resources.ps1 -OutputFile "my-resources.csv"

    Runs the script and saves Azure resources to "my-resources.csv".

.NOTES
    Requires:
      - PowerShell 7+
      - Azure CLI (`az` command)
      - Logged-in Azure account (`az login`)
#>

param (
    [string]$OutputFile = "azure-resources.powershell.csv",
    [string]$OutputHeaders = '"Location":location,"Name":name,"Resource Group":resourceGroup',
    [switch]$Help
)

## Display help message if -Help or -? is used
if ( $Help ) {
    Get-Help $PSCommandPath -Full ;
    exit ;
} ;

## Ensure Azure CLI is installed
Write-Host "`nChecking Azure CLI availability ..." `
           -ForegroundColor Cyan ;
if ( -not ( Get-Command az -ErrorAction SilentlyContinue ) ) {
    Write-Host "Error: Azure CLI is not installed or not found in PATH." `
               -ForegroundColor Red ;
    Write-Host "➡ Please install Azure CLI from https://aka.ms/installazurecli" ;
    exit 1 ;
} ;

## Ensure the user is logged in to Azure
Write-Host "Checking Azure authentication ..." `
           -ForegroundColor Cyan ;
if ( -not ( az account show 2>$null ) ) {
    Write-Error "You are not logged in to Azure. Run 'az login' and try again." ;
    exit 1 ;
} ;

## Extract only column names and enforce double quotes for all headers
Write-Host "`nParsing output headers ..." `
           -ForegroundColor Cyan ;
$ColumnNames = ( $OutputHeaders -split ',' ) -replace '"([^"]+)":.*', '$1' ;
$QuotedHeaders = $ColumnNames | ForEach-Object {
    if ($_ -match '^".*"$') { $_ } else { "`"$_`"" }
} ;

## Convert array to CSV format
$HeaderLine = $QuotedHeaders -join ',' ;

Write-Host "Writing headers to '${OutputFile}'..." `
           -ForegroundColor Cyan ;
## Write CSV header correctly
$HeaderLine | Set-Content -Path $OutputFile -Encoding utf8NoBOM;

Write-Host "`nFetching Azure resources from CLI ..." `
           -ForegroundColor Cyan ;
## Fetch Azure resources and append to CSV
try {
    $sortedData = az resource list --query "[].{${OutputHeaders}}" `
                                   --output tsv `
                | ForEach-Object {
                      ( $_ -replace "`t", '","' ) -replace '^(.*)$', '"$1"'
                  } `
                | Sort-Object -CaseSensitive ;
    # $sortedData | Out-File -FilePath $OutputFile `
    #                        -Append `
    #                        -Encoding utf8;
    $sortedData -replace "\r","" -replace '\s+$', "" `
    | Set-Content -Path $OutputFile `
                  -Append `
                  -Encoding utf8NoBOM;
    Write-Host "`nAzure resources successfully exported!" `
               -ForegroundColor Green;
    Write-Host "Saved as: $OutputFile" `
               -ForegroundColor Green;
} catch {
    Write-Error "`nFailed to fetch Azure resources. Please check your Azure configuration." ;
    exit 1 ;
} ;

## Display output
Write-Host "`nCSV Content (First 10 Lines):`n" `
           -ForegroundColor Yellow ;
Get-Content -Path $OutputFile `
| Select-Object -First 10 ;
