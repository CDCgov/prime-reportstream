# SFTP Module

This module sets up an SFTP server using Azure Storage Account's File Storage with SFTP support.

## Purpose

The SFTP module creates and configures an Azure Storage Account with SFTP capabilities, allowing secure file transfer for the ReportStream project.

## Resources Created

- Azure Storage Account with SFTP enabled
- Storage Container for SFTP
- Local user for SFTP access
- Network rules for SFTP access
- Key Vault Secret for SFTP password

## Usage

Include this module in your Terraform configuration:

``` hcl
module "sftp" {
  source = "./modules/sftp"

  environment                   = var.environment
  resource_group                = var.resource_group
  resource_prefix               = var.resource_prefix
  location                      = var.location
  sftp_user_name                = var.sftp_user_name
  sftp_password                 = var.sftp_password
  sftp_folder                   = var.sftp_folder
  key_vault_id                  = var.key_vault_id
  terraform_caller_ip_address   = var.terraform_caller_ip_address
  nat_gateway_id                = var.nat_gateway_id
  sshnames                      = var.sshnames
  sshinstances                  = var.sshinstances
  sftp_dir                      = var.sftp_dir
}
```

## Inputs

| Name | Description | Type | Default |
|------|-------------|------|---------|
| environment | Target Environment | string | |
| resource_group | Resource Group Name | string | |
| resource_prefix | Resource Prefix | string | |
| location | Function App Location | string | |
| sftp_user_name | Username for the SFTP site | string | "foo" |
| sftp_password | Password for the SFTP site | string | "bar" |
| sftp_folder | SFTP folder name | string | "default" |
| key_vault_id | Key Vault resource id | string | |
| terraform_caller_ip_address | The IP address of the Terraform script caller | list(string) | |
| nat_gateway_id | NAT gateway resource id | string | |
| sshnames | SSH Names | any | |
| sshinstances | SSH Instances | any | |
| sftp_dir | SFTP Script Directory | any | |

## Outputs

This module doesn't explicitly define outputs, but it creates several resources that can be referenced by other modules or used directly in your Terraform configuration.

## Security Considerations

- The SFTP password is stored securely in Azure Key Vault.
- Network rules are applied to restrict access to the SFTP server.
- The module uses the provided NAT gateway for outbound connections.

## Contributing

When modifying this module:

1. Follow Azure Storage and SFTP best practices.
2. Ensure changes align with ReportStream's security and compliance requirements.
3. Test thoroughly in a non-production environment before applying to production.

For more details on ReportStream's infrastructure, refer to the main project documentation.
