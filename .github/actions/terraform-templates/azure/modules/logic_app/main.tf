resource "azurerm_logic_app_workflow" "default" {
  name                = "la-${var.common.env}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  identity {
    type = "SystemAssigned"
  }

  lifecycle {
    ignore_changes = [parameters, workflow_parameters]
  }
}

data "azapi_resource" "logicapp_resource" {
  depends_on = [azurerm_logic_app_workflow.default, azurerm_resource_group_template_deployment.la_db_workflow]
  name       = azurerm_logic_app_workflow.default.name
  parent_id  = var.common.resource_group.id
  type       = "Microsoft.Logic/workflows@2019-05-01"

  response_export_values = ["*"]
}

resource "azurerm_logic_app_trigger_http_request" "default" {
  name         = "${var.common.env}-trigger"
  logic_app_id = azurerm_logic_app_workflow.default.id
  method       = "POST"

  schema = <<SCHEMA
{
  "properties": {
      "name": {
          "type": "string"
      }
  },
  "type": "object"
}
SCHEMA

  depends_on = [azurerm_resource_group_template_deployment.la_db_workflow]
  lifecycle {
    replace_triggered_by = [azurerm_resource_group_template_deployment.la_db_workflow]
  }
}

data "template_file" "la_db_workflow" {
  template = file("${path.module}/workflows/${var.workflow_file}")
}

resource "azurerm_resource_group_template_deployment" "la_db_workflow" {
  depends_on = [azurerm_logic_app_workflow.default]

  name                = "la-${var.common.env}-workflow"
  resource_group_name = var.common.resource_group.name
  deployment_mode     = "Incremental"
  parameters_content = jsonencode({
    "workflows_la_name" = {
      value = azurerm_logic_app_workflow.default.name
    }
    "location" = {
      value = var.common.location
    }
    "sql_api_id" = {
      value = data.azurerm_managed_api.sql.id
    }
    "sql_conn_id" = {
      value = azapi_resource.sql.id
    }
    "sql_conn_name" = {
      value = azapi_resource.sql.name
    }
    "sql_server_fqdn" = {
      value = var.sql_server_fqdn
    }
    "db_name" = {
      value = var.db_name
    }
    "query" = {
      value = "SELECT * FROM sys.objects"
    }
  })

  template_content = data.template_file.la_db_workflow.template
}

data "azurerm_managed_api" "sql" {
  name     = "sql"
  location = var.common.location
}

resource "azapi_resource" "sql" {
  type      = "Microsoft.Web/connections@2016-06-01"
  name      = "sql-la-db"
  parent_id = var.common.resource_group.id
  location  = var.common.location

  body = jsonencode({
    properties = {
      api = {
        id = data.azurerm_managed_api.sql.id
      }
      displayName = "sql-la-db"
      parameterValueSet = {
        name   = "oauthMI"
        values = {}
      }
    }
  })
  schema_validation_enabled = false
}