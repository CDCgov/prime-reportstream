# Application Insights Module

This module provisions and configures Azure Application Insights for the ReportStream project.

## Purpose

The application_insights module sets up monitoring and telemetry for ReportStream applications, providing insights into performance, usage, and potential issues.

## Resources Created

- Azure Application Insights
- Action Groups for alerts
- Metric Alerts
- Availability Tests

## Usage

Include this module in your Terraform configuration:

``` hcl
module "application_insights" {
  source = "./application_insights"

  environment         = var.environment
  resource_group      = var.resource_group
  location            = var.location
  resource_prefix     = var.resource_prefix
  is_metabase_env     = var.is_metabase_env
  pagerduty_url       = var.pagerduty_url
  slack_email_address = var.slack_email_address
  postgres_server_id  = var.postgres_server_id
  service_plan_id     = var.service_plan_id
  workspace_id        = var.workspace_id
}
```

## Inputs

| Name | Description | Type | Required |
|------|-------------|------|:--------:|
| environment | Target Environment | string | yes |
| resource_group | Resource Group Name | string | yes |
| location | Function App Location | string | yes |
| resource_prefix | Resource Prefix | string | yes |
| is_metabase_env | Should Metabase be deployed in this environment | bool | yes |
| pagerduty_url | PagerDuty URL for alerts | any | yes |
| slack_email_address | Slack email address for notifications | any | yes |
| postgres_server_id | PostgreSQL Server ID | any | yes |
| service_plan_id | Service Plan ID | any | yes |
| workspace_id | Log Analytics Workspace resource id | string | yes |

## Features

- Creates Application Insights instance
- Sets up action groups for PagerDuty and Slack notifications
- Configures metric alerts for various performance and health indicators
- Establishes availability tests for key endpoints

## Outputs

- `instrumentation_key`: The instrumentation key for the Application Insights instance
- `app_id`: The App ID of the Application Insights instance

## Notes

- Ensure that the necessary permissions are in place for creating and managing Application Insights resources.
- Review and adjust alert thresholds as needed for your specific environment and requirements.
- The availability tests are configured for specific endpoints; update these as necessary for your application.

For more details on ReportStream's monitoring strategy and infrastructure, refer to the main project documentation.
