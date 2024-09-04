# Function App Module

This module deploys and configures Azure Function Apps for the ReportStream project.

## Purpose

The function_app module sets up the serverless compute environment for running ReportStream's backend functions, including necessary dependencies and configurations.

## Resources Created

- Azure Function App
- App Service Plan
- Application Insights integration
- Storage account connections
- Key Vault access
- Virtual Network integration
- IP restrictions

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your Terraform file:

``` hcl
module "function_app" {
  source = "./modules/function_app"
  
  environment = var.environment
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
  location = var.location
  // Add other required variables
}
```

2. Provide all necessary variables as defined in the Inputs section.

## Inputs

| Name | Description | Type | Required |
|------|-------------|------|:--------:|
| environment | Target Environment | string | yes |
| resource_group | Resource Group Name | string | yes |
| resource_prefix | Resource Prefix | string | yes |
| location | Function App Location | string | yes |
| ai_instrumentation_key | Application Insights Instrumentation Key | string | yes |
| ai_connection_string | Application Insights Connection String | string | yes |
| okta_redirect_url | Okta Redirect URL | string | yes |
| RS_okta_redirect_url | Okta Redirect URL | string | yes |
| terraform_caller_ip_address | The IP address of the Terraform script caller | list(string) | yes |
| use_cdc_managed_vnet | If the environment should be deployed to the CDC managed VNET | bool | yes |
| pagerduty_url | PagerDuty URL | string | yes |
| app_service_plan | App Service Plan | any | yes |
| storage_account | Storage Account | any | yes |
| primary_access_key | Primary Access Key | string | yes |
| candidate_access_key | Candidate Access Key | string | yes |
| container_registry_login_server | Container Registry Login Server | string | yes |
| container_registry_admin_username | Container Registry Admin Username | string | yes |
| container_registry_admin_password | Container Registry Admin Password | string | yes |
| primary_connection_string | Primary Connection String | string | yes |
| postgres_user | PostgreSQL User | string | yes |
| postgres_pass | PostgreSQL Password | string | yes |
| application_key_vault_id | Application Key Vault ID | string | yes |
| sa_partner_connection_string | SA Partner Connection String | string | yes |
| client_config_key_vault_id | Client Config Key Vault ID | string | yes |
| client_config_key_vault_name | Client Config Key Vault Name | string | yes |
| app_config_key_vault_id | App Config Key Vault ID | string | yes |
| app_config_key_vault_name | App Config Key Vault Name | string | yes |
| dns_ip | DNS IP | string | yes |
| okta_base_url | Okta Base URL | string | yes |
| OKTA_authKey | Okta Auth Key | string | yes |
| OKTA_clientId | Okta Client ID | string | yes |
| OKTA_scope | Okta Scope | string | yes |
| RS_okta_base_url | RS Okta Base URL | string | yes |
| RS_OKTA_authKey | RS Okta Auth Key | string | yes |
| RS_OKTA_clientId | RS Okta Client ID | string | yes |
| RS_OKTA_scope | RS Okta Scope | string | yes |
| etor_ti_base_url | ETOR TI Base URL | string | yes |
| subnets | A set of all available subnet combinations | any | yes |
| is_temp_env | Is a temporary environment | bool | no |
| function_runtime_version | Function app runtime version | string | yes |

## Outputs

The module provides several outputs, including:

- Function App name and ID
- Function App default hostname
- App Service Plan ID

## Security Features

- System-assigned managed identity for secure access to Azure resources
- Virtual Network integration for enhanced network security
- IP restrictions to control access to the Function App

## Contributing

When modifying this module:

1. Follow Azure Function App best practices and Terraform conventions.
2. Test thoroughly in a non-production environment before applying changes to production.
3. Update this README to reflect any changes in inputs, outputs, or functionality.

For more details on ReportStream's infrastructure, refer to the main project documentation.
