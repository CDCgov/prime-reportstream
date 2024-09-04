# Storage Module

This module manages the storage infrastructure for the ReportStream project.

## Purpose

The storage module is responsible for provisioning and configuring Azure Storage resources required by ReportStream. It sets up storage accounts, containers, and related components to support the application's data storage needs.

## Key Components

- Azure Storage Account
- Storage Containers
- Storage Queue
- Private Endpoints
- Network Rules

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "storage" {
  source = "./storage"
  
  environment                    = var.environment
  resource_group                 = var.resource_group
  resource_prefix                = var.resource_prefix
  location                       = var.location
  rsa_key_4096                   = var.rsa_key_4096
  terraform_caller_ip_address    = var.terraform_caller_ip_address
  use_cdc_managed_vnet           = var.use_cdc_managed_vnet
  application_key_vault_id       = var.application_key_vault_id
  dns_vnet                       = var.dns_vnet
  subnets                        = var.subnets
  dns_zones                      = var.dns_zones
  delete_pii_storage_after_days  = var.delete_pii_storage_after_days
  is_temp_env                    = var.is_temp_env
  storage_queue_name             = var.storage_queue_name
}
```

2. Provide all necessary variables as defined in the `~inputs.tf` file.

3. Execute the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Module Inputs

The following inputs are required by this module:

- `environment`: Target Environment
- `resource_group`: Resource Group Name
- `resource_prefix`: Resource Prefix
- `location`: Storage Account Location
- `rsa_key_4096`: Name of the 4096 length RSA key in the Key Vault
- `terraform_caller_ip_address`: The IP address of the Terraform script caller
- `use_cdc_managed_vnet`: If the environment should be deployed to the CDC managed VNET
- `application_key_vault_id`: Application Key Vault ID
- `dns_vnet`: DNS VNET
- `subnets`: A set of all available subnet combinations
- `dns_zones`: A set of all available dns zones
- `delete_pii_storage_after_days`: Number of days after which PII-related blobs will be deleted
- `is_temp_env`: Is a temporary environment (default: false)
- `storage_queue_name`: Default storage queue names (default: ["proces"])

## Configuration Details

- The module creates an Azure Storage Account with appropriate configurations based on the environment.
- Storage containers are created for various purposes.
- A storage queue is set up for processing tasks.
- Private endpoints are configured for secure access within the virtual network.
- Network rules are applied to restrict access to the storage account.

## Security Considerations

- Access to the storage account is restricted by network rules and private endpoints.
- Customer-managed keys can be used for encryption if specified.
- PII data is automatically deleted after a specified number of days.

## Contributing

When working on this module:

1. Adhere to Azure Storage best practices and Terraform standards.
2. Ensure any changes align with ReportStream's data architecture and security requirements.
3. Thoroughly test all modifications in a non-production environment before applying to production.

For more information on ReportStream's storage infrastructure, consult the main project documentation.
