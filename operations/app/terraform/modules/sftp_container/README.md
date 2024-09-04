# SFTP Container Module

This module deploys and configures an SFTP container in Azure for the ReportStream project.

## Purpose

The sftp_container module sets up a secure file transfer protocol (SFTP) container in Azure, allowing for secure file transfers within the ReportStream infrastructure.

## Resources Created

- Azure Container Group
- Network Profile
- Private DNS Zone
- Private DNS Zone Virtual Network Link
- Network Interface
- Public IP Address

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your Terraform file:

``` hcl
module "sftp_container" {
  source = "./modules/sftp_container"

  environment           = var.environment
  resource_group        = var.resource_group
  resource_prefix       = var.resource_prefix
  location              = var.location
  use_cdc_managed_vnet  = var.use_cdc_managed_vnet
  sa_primary_access_key = var.sa_primary_access_key
  dns_zones             = var.dns_zones
  storage_account       = var.storage_account
}
```

2. Ensure all required variables are provided.

3. Run the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Input Variables

| Name | Description | Type |
|------|-------------|------|
| environment | Target Environment | string |
| resource_group | Resource Group Name | string |
| resource_prefix | Resource Prefix | string |
| location | Function App Location | string |
| use_cdc_managed_vnet | If the environment should be deployed to the CDC managed VNET | bool |
| sa_primary_access_key | Storage Account Primary Access Key | any |
| dns_zones | A set of all available DNS zones | any |
| storage_account | Storage Account details | any |

## Key Features

- Deploys an Azure Container Group for SFTP services
- Configures networking with a private DNS zone
- Sets up necessary network interfaces and public IP
- Integrates with existing storage account for file storage

## Security Considerations

- The module uses private DNS zones for enhanced security
- Network security group rules should be reviewed and adjusted as needed
- Access to the SFTP container should be monitored and logged

## Contributing

When modifying this module:

1. Follow Azure Container Instances and SFTP best practices
2. Ensure changes align with ReportStream's security and compliance requirements
3. Test thoroughly in a non-production environment before applying to production

For more details on ReportStream's infrastructure, refer to the main project documentation.
