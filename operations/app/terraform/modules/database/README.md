# Database Module

This module manages the PostgreSQL database infrastructure for the ReportStream project.

## Purpose

The database module provisions and configures Azure Database for PostgreSQL instances and related components to support ReportStream's data storage needs.

## Key Components

- Azure Database for PostgreSQL
- Firewall rules
- Private DNS zones
- Virtual Network integration
- Flexible server instances (optional)

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "database" {
  source = "./database"
  
  environment                 = var.environment
  resource_group              = var.resource_group
  resource_prefix             = var.resource_prefix
  location                    = var.location
  rsa_key_2048                = var.rsa_key_2048
  aad_group_postgres_admin    = var.aad_group_postgres_admin
  is_metabase_env             = var.is_metabase_env
  use_cdc_managed_vnet        = var.use_cdc_managed_vnet
  postgres_user               = var.postgres_user
  postgres_pass               = var.postgres_pass
  postgres_readonly_user      = var.postgres_readonly_user
  postgres_readonly_pass      = var.postgres_readonly_pass
  db_sku_name                 = var.db_sku_name
  db_version                  = var.db_version
  db_storage_mb               = var.db_storage_mb
  db_auto_grow                = var.db_auto_grow
  db_prevent_destroy          = var.db_prevent_destroy
  db_threat_detection         = var.db_threat_detection
  db_replica                  = var.db_replica
  application_key_vault_id    = var.application_key_vault_id
  dns_vnet                    = var.dns_vnet
  subnets                     = var.subnets
  dns_zones                   = var.dns_zones
  flex_instances              = var.flex_instances
  flex_sku_name               = var.flex_sku_name
}
```

2. Provide all necessary variables through variable files or command-line arguments.

3. Execute the Terraform workflow:

``` bash
terraform init
terraform plan
terraform apply
```

## Configuration Details

- Creates an Azure Database for PostgreSQL with flexible server options
- Sets up firewall rules for secure access
- Configures private DNS zones for network integration
- Optionally creates flexible server instances
- Integrates with Azure Key Vault for secure key management
- Configures threat detection and replica if specified

## Inputs

| Name | Description | Type | Default |
|------|-------------|------|---------|
| environment | Target Environment | string | |
| resource_group | Resource Group Name | string | |
| resource_prefix | Resource Prefix | string | |
| location | Database Server Location | string | |
| rsa_key_2048 | Name of the 2048 length RSA key in the Key Vault | string | |
| aad_group_postgres_admin | Azure Active Directory Group ID for postgres_admin | string | |
| is_metabase_env | Should Metabase be deployed in this environment | bool | |
| use_cdc_managed_vnet | If the environment should be deployed to the CDC managed VNET | bool | |
| postgres_user | PostgreSQL user | string | |
| postgres_pass | PostgreSQL password | string | |
| postgres_readonly_user | PostgreSQL read-only user | string | |
| postgres_readonly_pass | PostgreSQL read-only password | string | |
| db_sku_name | Database SKU name | string | |
| db_version | Database version | string | |
| db_storage_mb | Database storage in MB | number | |
| db_auto_grow | Enable auto-grow for database | bool | |
| db_prevent_destroy | Prevent destruction of the database | bool | |
| db_threat_detection | Enable threat detection | bool | |
| db_replica | Enable database replica | bool | |
| application_key_vault_id | Application Key Vault ID | string | |
| dns_vnet | DNS Virtual Network | object | |
| subnets | A set of all available subnet combinations | map | |
| dns_zones | A set of all available DNS zones | map | |
| flex_instances | Flexible server instances | list | [] |
| flex_sku_name | Flexible server SKU name | string | "GP_Standard_D4ds_v4" |

## Outputs

The module provides several outputs, including database connection strings, server names, and resource IDs.

## Security Considerations

- Access to the database is restricted by firewall rules and network integration
- Sensitive information is stored in Azure Key Vault
- Threat detection can be enabled for enhanced security

## Contributing

When working on this module:

1. Follow Azure Database for PostgreSQL best practices and Terraform standards
2. Ensure changes align with ReportStream's data architecture and security requirements
3. Test all modifications thoroughly in a non-production environment

For more information on ReportStream's data infrastructure, consult the main project documentation.
