# Sync Azure Blob Storage for BC/DR using AzCopy

# Configuration - Correct account names and container name as confirmed
$primaryAccount = "<primaty-account>"            # Primary storage account (Standard_GRS)
$secondaryAccount = "<secondary-account>"       # Secondary storage account (Standard_RAGRS)
$resourceGroup = "prime-data-hub-prod"       # Resource group name
$container = "<container>"                # Blob container name

# Obtain Primary Storage Account Key
$primaryKey = $(az storage account keys list --account-name $primaryAccount --resource-group $resourceGroup --query '[0].value' -o tsv)
# Obtain Secondary Storage Account Key
$secondaryKey = $(az storage account keys list --account-name $secondaryAccount --resource-group $resourceGroup --query '[0].value' -o tsv)

# Display Sync Start Message
Write-Output "Initiating blob storage sync from primary to secondary..."

# Perform Blob Data Sync from Primary to Secondary using AzCopy
azcopy sync "https://$primaryAccount.blob.core.windows.net/$container" `
"https://$secondaryAccount.blob.core.windows.net/$container" `
--source-key=$primaryKey --dest-key=$secondaryKey --recursive

# Display Sync Completion Message
if ($?) {
    Write-Output "Blob storage sync from primary to secondary completed successfully."
} else {
    Write-Output "Error: Blob storage sync failed. Please check the logs for details."
}

