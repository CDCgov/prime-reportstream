# Log Analytics Workspace Module

This module manages the Log Analytics Workspace and related resources for the ReportStream project.

## Purpose

The log_analytics_workspace module is responsible for creating and configuring an Azure Log Analytics Workspace, which serves as a centralized logging and monitoring solution for the ReportStream infrastructure.

## Key Components

- Azure Log Analytics Workspace
- Diagnostic settings for various Azure resources
- Alert rules and action groups

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "log_analytics_workspace" {
  source = "./log_analytics_workspace"
  
  environment                = var.environment
  resource_group             = var.resource_group
  resource_prefix            = var.resource_prefix
  location                   = var.location
  service_plan_id            = var.service_plan_id
  container_registry_id      = var.container_registry_id
  postgres_server_id         = var.postgres_server_id
  application_key_vault_id   = var.application_key_vault_id
  app_config_key_vault_id    = var.app_config_key_vault_id
  client_config_key_vault_id = var.client_config_key_vault_id
  function_app_id            = var.function_app_id
  front_door_id              = var.front_door_id
  nat_gateway_id             = var.nat_gateway_id
  primary_vnet_id            = var.primary_vnet_id
  replica_vnet_id            = var.replica_vnet_id
  storage_account_id         = var.storage_account_id
  storage_public_id          = var.storage_public_id
  storage_partner_id         = var.storage_partner_id
  action_group_slack_id      = var.action_group_slack_id
  action_group_metabase_id   = var.action_group_metabase_id
  data_factory_id            = var.data_factory_id
  sftp_instance_01_id        = var.sftp_instance_01_id
  law_retention_period       = var.law_retention_period
  action_group_id            = var.action_group_id
}
```

2. Ensure all required variables are provided.

3. Run the standard Terraform workflow.

## Module Inputs

The module accepts the following inputs:

- `environment`: Target Environment
- `resource_group`: Resource Group Name
- `resource_prefix`: Resource Prefix
- `location`: Function App Location
- `service_plan_id`: Application Service Plan resource id
- `container_registry_id`: Container Registry resource id
- `postgres_server_id`: Postgres Server resource id
- `application_key_vault_id`: App Key Vault resource id
- `app_config_key_vault_id`: App Config Key Vault resource id
- `client_config_key_vault_id`: Client Config Key Vault resource id
- `function_app_id`: Function App resource id
- `front_door_id`: Front Door resource id (optional)
- `nat_gateway_id`: Nat Gateway resource id
- `primary_vnet_id`: Primary vnet resource id
- `replica_vnet_id`: Replica vnet resource id
- `storage_account_id`: Storage Account resource id
- `storage_public_id`: Storage Public resource id
- `storage_partner_id`: Storage Partner resource id
- `action_group_slack_id`: Slack action group resource id
- `action_group_metabase_id`: Metabase healthcheck action group resource id
- `data_factory_id`: Data factory resource id (optional)
- `sftp_instance_01_id`: SFTP instance 01 resource id (optional)
- `law_retention_period`: How long to keep logs for in LAW (default: "30")
- `action_group_id`: Action group resource id

## Key Features

- Creates a Log Analytics Workspace with specified retention period
- Sets up diagnostic settings for various Azure resources
- Configures alert rules for monitoring critical metrics
- Integrates with action groups for notifications

## Outputs

The module provides outputs such as the Log Analytics Workspace ID and primary shared key, which can be used by other modules or for reference.

## Contributing

When modifying this module:

1. Follow Azure Log Analytics best practices and Terraform conventions.
2. Ensure any changes align with ReportStream's monitoring and logging requirements.
3. Test thoroughly in a non-production environment before applying to production.

For more details on ReportStream's monitoring infrastructure, refer to the main project documentation.
