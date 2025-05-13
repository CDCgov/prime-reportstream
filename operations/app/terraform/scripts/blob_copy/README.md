3. The script will copy data from the primary blob container (`terraformstate`) to the secondary container.

## What You Need
- Azure CLI
- AzCopy
- Access to both storage accounts

## How It’s Set Up
- **Primary Account:** pdhprodpublic (Standard_GRS)
- **Secondary Account:** pdhprodterraform (Standard_RAGRS)
- **Blob Container:** terraformstate
- **Resource Group:** prime-data-hub-prod

## What to Expect
- If the sync works, you’ll see:

