# App Service Plan Module

This module creates and manages an Azure App Service Plan for the ReportStream project.

## Purpose

The app_service_plan module is responsible for provisioning the Azure App Service Plan, which defines the compute resources for hosting web applications, APIs, and Azure Functions.

## Resources Created

- Azure App Service Plan

## Usage

To use this module in your Terraform configuration:

1. Reference this module in your main Terraform file:

``` hcl
module "app_service_plan" {
  source = "./app_service_plan"
  
  environment     = var.environment
  resource_group  = var.resource_group
  resource_prefix = var.resource_prefix
  location        = var.location
  app_size        = var.app_size
  app_tier        = var.app_tier
}
```

2. Provide the required variables through variable files or command-line arguments.

## Input Variables

| Name | Description | Type | Required |
|------|-------------|------|:--------:|
| environment | The deployment environment (e.g., dev, staging, prod) | string | Yes |
| resource_group | The name of the resource group | string | Yes |
| resource_prefix | Prefix used for resource naming | string | Yes |
| location | Azure region where resources will be created | string | Yes |
| app_size | The size of the App Service Plan (e.g., S1, P1v2) | string | Yes |
| app_tier | The tier of the App Service Plan (e.g., Standard, PremiumV2) | string | Yes |

## Outputs

This module provides the following outputs:

- `app_service_plan_id`: The ID of the created App Service Plan
- `app_service_plan_name`: The name of the created App Service Plan

## Contributing

When modifying this module:

1. Follow Azure App Service Plan best practices and Terraform conventions.
2. Ensure changes align with ReportStream's infrastructure requirements.
3. Test thoroughly in a non-production environment before applying to production.

For more details on ReportStream's infrastructure, refer to the main project documentation.
