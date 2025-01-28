#!/usr/bin/env pwsh

# Define parameters
param (
    [string]$OutputFile = "azure-resources--powershell.csv"
)

# Output header
"Location,Name,Resource Group" | Out-File -FilePath "azure-resources.csv" -Encoding utf8 ;

# Fetch Azure resources and append to CSV
az resource list --query '[].{"Location":location,"Name":name,"Resource Group":resourceGroup}' --output tsv |
    ForEach-Object { $_ -replace "`t", "," } |
    Out-File -FilePath $OutputFile -Append -Encoding utf8 ;

# Display the contents of the generated CSV
Get-Content -Path $OutputFile ;
