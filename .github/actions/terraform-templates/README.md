---
runme:
  id: 01HVWMC7W5WGR5QF3H8KNK4JP4
  version: v3
---

# Terraform Templates

## Introduction

This repository contains Terraform templates for deploying various resources in Azure. It is designed to help beginners get started with provisioning infrastructure using Terraform.

## Prerequisites

- Azure CLI installed
- Azure account with sufficient permissions
- Terraform installed

## Getting Started

> I recommend running in Codespaces or Dev Container in VS Code:
>
> [![Open in GitHub Codespaces](https://github.com/codespaces/badge.svg)](https://codespaces.new/JosiahSiegel/terraform-templates)

### 1. Login to Azure

```sh {"id":"01HVWMC7W5WGR5QF3H80KE8XAZ"}
az login
```

or

```sh {"id":"01HVWPXVEB7ZFQETZD308G7HXP"}
az login --use-device-code
```

### 2. Export Owner Object ID

Export an owner object ID to a variable by running the following command:

```sh {"id":"01HVWMC7W5WGR5QF3H83QR73CK"}
read -p "Enter AD user display name [Smith, Joe]: " display_name
owner_object_id=$(az ad user list --filter "displayname eq '$display_name'" --query '[0].id' -o tsv)
export owner_object_id=$owner_object_id
echo "Object Id: $owner_object_id"
```

### 3. Create Terraform Backend

Create the Terraform state backend file and storage account for a template by running the following script:

```sh {"id":"01HVWMC7W5WGR5QF3H85WFTTP6"}
source ./.scripts/provision_template.sh
```

> Optional: set `template_path=azure/env/<template>` if backend already exists:

```sh {"id":"01HW67SE89X56V69ASEFGZB6AX"}
read -p "Enter the template environment: " template
export template_path="azure/env/${template:-_}"
if [ ! -d "$template_path" ]; then
echo "Error: The specified template directory '$template_path' does not exist."
else
echo "Set template environment '$template_path'!"
fi
```

### 4. Initialize Terraform

Initialize Terraform using the desired template path (`azure/env/<template>`) by running:

```sh {"id":"01HVWMC7W5WGR5QF3H89NDQK07"}
terraform -chdir=$template_path init -reconfigure
```

Replace 01 with the appropriate template directory.

### 5. Plan Terraform Deployment

Generate an execution plan for the Terraform deployment by running:

```sh {"id":"01HVWMC7W5WGR5QF3H8BHW8T14"}
terraform -chdir=$template_path plan
```

### 6. Apply Terraform Deployment

```sh {"id":"01HVWMC7W5WGR5QF3H8DQ9WBBE"}
terraform -chdir=$template_path apply
```

If a login failure occurs with the error "Login failed for user ''", try running `az logout` and then `az login` to resolve the issue.

### 7. Destroy the Terraform deployment and terraform storage account

```sh {"id":"01HVWMC7W5WGR5QF3H8GNPS01Z"}
./.scripts/destroy_template.sh
```

## Available Templates

### Template 01

|Azure Resource|Purpose|
|---|---|
|API Management|Securely share and manage REST endpoints|
|Logic App|Make database data available via REST endpoint|
|Data Factory|Ingest APIs and store data in a database and storage account|
|Key Vault|Manage secrets|
|Storage Account|Store results from ingested APIs|
|Azure SQL DB|Store data from ingested APIs and expose via Logic App and API Management|

### Template 02

|Azure Resource|Purpose|
|---|---|
|Key Vault|Manage secrets|
|Azure SQL DB|Store data|

### Template 03

|Azure Resource|Purpose|
|---|---|
|CosmosDB for PostgreSQL||
|VNet|Private network|
|Private Endpoint|CosmosDB traffic over vnet|
|Key Vault|CosmosDB password|
|Container Instance|Run DB backup/restore to file share|
|Storage Account|File share storage for container instance|

### Template 04

|Azure Resource|Purpose|
|---|---|
pending...||

### Template 05

|Azure Resource|Purpose|
|---|---|
|Container Instance|Linux container with file share mount|
|Storage Account|File share storage for container instance|

### Template 06

|Azure Resource|Purpose|
|---|---|
|Container Instance|Windows container|

## Troubleshooting

1. If login failure during plan or apply, try running `az logout` then `az login`
2. If you encounter any issues, please check the Terraform state backend and `_override.tf.json` files and storage account created in step 3
3. If error "SubscriptionNotFound", manually create a single resource (other than a Resource Group) if non-exist

## Contributing

Feel free to submit pull requests or open issues if you have any suggestions or improvements for these Terraform templates.

### Bonus

 * Run scripts and enter terminal for container instance

```sh {"id":"01HWZY6RPNNYHM0MN1F6MF9QZB"}
az container exec --name cinst-dev --resource-group demo --container-name cinst-dev --exec-command "/bin/bash -c /app/repo1/terraform-templates/.scripts/utils/psql_install_16.sh"
```

```sh {"id":"01HWY6JGNS1CTNGATQE5KMKNCR"}
az container exec --name cinst-dev --resource-group demo --container-name cinst-dev --exec-command "/bin/bash"
```

* Add existing public SSH key to container instance

```sh
ssh-copy-id -p 2222 -i ~/.ssh/id_ed25519.pub app@cinst-dev.eastus.azurecontainer.io
```
