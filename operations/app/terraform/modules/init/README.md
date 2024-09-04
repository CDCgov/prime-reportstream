# Init Module

This module initializes the core infrastructure components for the ReportStream project.

## Purpose

The init module sets up foundational Azure resources required for the ReportStream application. It creates essential components such as resource groups, key vaults, and networking infrastructure.

## Resources Created

- Azure Resource Group
- Azure Key Vaults (for application, app config, and client config)
- Virtual Network and Subnets
- Network Security Groups
- Private DNS Zones
- Private Endpoints

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "init" {
  source = "./init"
  
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = var.location
  aad_object_keyvault_admin = var.aad_object_keyvault_admin
  terraform_caller_ip_address = var.terraform_caller_ip_address
  use_cdc_managed_vnet = var.use_cdc_managed_vnet
  terraform_object_id = var.terraform_object_id
  app_config_kv_name = var.app_config_kv_name
  application_kv_name = var.application_kv_name
  dns_vnet = var.dns_vnet
  client_config_kv_name = var.client_config_kv_name
  network = var.network
  subnets = var.subnets
  random_id = var.random_id
}
```

2. Ensure all required variables are provided.

3. Run the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Inputs

| Name | Description | Type | Default |
|------|-------------|------|---------|
| environment | The deployment environment | string | |
| resource_group | The name of the resource group | string | |
| resource_prefix | Prefix for resource names | string | |
| location | Azure region for resource deployment | string | |
| aad_object_keyvault_admin | AAD object ID for Key Vault admin | string | |
| terraform_caller_ip_address | IP address of the Terraform caller | string | |
| use_cdc_managed_vnet | Flag to use CDC managed VNet | bool | |
| terraform_object_id | Object ID for Terraform | string | |
| app_config_kv_name | Name of the app config Key Vault | string | |
| application_kv_name | Name of the application Key Vault | string | |
| dns_vnet | DNS VNet configuration | any | |
| client_config_kv_name | Name of the client config Key Vault | string | |
| network | Network configuration | any | |
| subnets | Available subnet combinations | string | "" |
| random_id | Random ID for resource naming | string | |

## Outputs

(List key outputs from the module, such as resource IDs or names)

## Notes

- This module sets up core infrastructure, so changes should be made carefully.
- Ensure proper access controls and network security settings are in place.
- Review and adjust Key Vault policies as needed for your specific use case.

For more details on ReportStream's infrastructure, refer to the main project documentation.
