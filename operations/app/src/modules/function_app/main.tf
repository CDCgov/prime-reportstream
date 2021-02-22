terraform {
    required_version = ">= 0.14"
}

resource "azurerm_app_service_plan" "service_plan" {
  name = "${var.resource_prefix}-serviceplan"
  location = var.location
  resource_group_name = var.resource_group
  kind = (var.environment == "prod" ? "elastic" : "Linux")
  reserved = true
  maximum_elastic_worker_count = (var.environment == "prod" ? 10 : 1)

  sku {
    tier = (var.environment == "prod" ? "ElasticPremium" : "PremiumV2")
    size = (var.environment == "prod" ? "EP1" : "P2v2")
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_function_app" "function_app" {
  name = "${var.resource_prefix}-functionapp"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = azurerm_app_service_plan.service_plan.id
  storage_account_name = var.storage_account_name
  storage_account_access_key = var.storage_account_key
  https_only = true
  os_type = "linux"
  version = "~3"
  enable_builtin_logging = false

  site_config {
    ip_restriction {
      action = "Allow"
      name = "AllowVNetTraffic"
      priority = 100
      virtual_network_subnet_id = var.public_subnet_id
    }
    ip_restriction {
      action = "Allow"
      name = "AllowFrontDoorTraffic"
      priority = 110
      service_tag = "AzureFrontDoor.Backend"
    }
    ip_restriction {
      action = "Allow"
      name = "jduff"
      priority = 120
      ip_address = "108.51.58.151/32"
    }
    scm_use_main_ip_restriction = true

    http2_enabled = true
    always_on = (var.environment == "prod" ? null : true)
    use_32_bit_worker_process = false
    linux_fx_version = "DOCKER|${var.login_server}/${var.resource_prefix}:latest"
  }

  app_settings = {
    "POSTGRES_USER" = var.postgres_user
    "POSTGRES_PASSWORD" = var.postgres_password
    "POSTGRES_URL" = var.postgres_url

    "PRIME_ENVIRONMENT" = (var.environment == "prod" ? "prod" : "test")

    "REDOX_SECRET" = var.redox_secret

    "OKTA_baseUrl" = "hhs-prime.okta.com"
    "OKTA_clientId" = var.okta_client_id
    "OKTA_redirect" = (var.environment == "prod" ? "https://prime.cdc.gov/download" : "https://prime-data-hub-test.azurefd.net/api/download")

    # Test and Prod both need each set of credentials for various
    # means of testing configurations
    "AZ_PHD__ELR_HL7_TEST__USER" = var.az_phd_user
    "AZ_PHD__ELR_HL7_TEST__PASS" = var.az_phd_password
    "AZ_PHD__ELR_TEST__USER" = var.az_phd_user
    "AZ_PHD__ELR_TEST__PASS" = var.az_phd_password

    "AZ_PHD__ELR_PROD__USER" = var.az_phd_user
    "AZ_PHD__ELR_PROD__PASS" = var.az_phd_password
    "AZ_PHD__ELR_HL7_PROD__USER" = var.az_phd_user
    "AZ_PHD__ELR_HL7_PROD__PASS" = var.az_phd_password

    "WEBSITE_VNET_ROUTE_ALL" = 1

    "DOCKER_REGISTRY_SERVER_URL" = var.login_server
    "DOCKER_REGISTRY_SERVER_USERNAME" = var.admin_user
    "DOCKER_REGISTRY_SERVER_PASSWORD" = var.admin_password
    "DOCKER_CUSTOM_IMAGE_NAME" = "${var.login_server}/${var.resource_prefix}:latest"

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false

    "APPINSIGHTS_INSTRUMENTATIONKEY" = var.ai_instrumentation_key

    "FUNCTION_APP_EDIT_MODE" = (var.environment == "prod" ? "readOnly" : null)
    "MACHINEKEY_DecryptionKey" = (var.environment == "prod" ? data.azurerm_function_app.app_data.app_settings.MACHINEKEY_DecryptionKey : null)
    "WEBSITE_CONTENTAZUREFILECONNECTIONSTRING" = (var.environment == "prod" ? data.azurerm_function_app.app_data.app_settings.WEBSITE_CONTENTAZUREFILECONNECTIONSTRING : null)
    "WEBSITE_CONTENTSHARE" = (var.environment == "prod" ? "${var.resource_prefix}-functionapp" : null)
    "WEBSITE_HTTPLOGGING_RETENTION_DAYS" = (var.environment == "prod" ? 3 : null)
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id = var.public_subnet_id
}

data "azurerm_function_app" "app_data" {
  name = "${var.resource_prefix}-functionapp"
  resource_group_name = var.resource_group
}

module "functionapp_app_log_event_hub_log" {
  source = "../event_hub_log"
  resource_type = "function_app"
  log_type = "app"
  eventhub_namespace_name = var.eventhub_namespace_name
  resource_group = var.resource_group
  resource_prefix = var.resource_prefix
}

resource "azurerm_monitor_diagnostic_setting" "functionapp_app_log" {
  name = "${var.resource_prefix}-function_app-app-log"
  target_resource_id = azurerm_function_app.function_app.id
  eventhub_name = module.functionapp_app_log_event_hub_log.event_hub_name
  eventhub_authorization_rule_id = var.eventhub_manage_auth_rule_id

  log {
    category = "FunctionAppLogs"
    enabled  = true

    retention_policy {
      days = 0
      enabled = false
    }
  }

  metric {
    category = "AllMetrics"
    enabled = false

    retention_policy {
      days = 0
      enabled = false
    }
  }
}

output "app_service_plan_id" {
  value = azurerm_app_service_plan.service_plan.id
}
