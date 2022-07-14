locals {
  slots = {
    active : azurerm_function_app.function_app
    candidate : azurerm_function_app_slot.candidate
  }
}

resource "azurerm_function_app_slot" "candidate" {
  function_app_name          = azurerm_function_app.function_app.name
  name                       = "candidate"
  location                   = var.location
  resource_group_name        = var.resource_group
  app_service_plan_id        = var.app_service_plan
  storage_account_name       = "${var.resource_prefix}candidate"
  storage_account_access_key = var.primary_access_key
  https_only                 = true
  os_type                    = "linux"
  version                    = var.function_runtime_version
  enable_builtin_logging     = false

  site_config {
    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[2]
    }

    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetEastTraffic"
      priority                  = 100
      virtual_network_subnet_id = var.subnets.public_subnets[0]
    }

    ip_restriction {
      action      = "Allow"
      name        = "AllowFrontDoorTraffic"
      priority    = 110
      service_tag = "AzureFrontDoor.Backend"
    }

    scm_use_main_ip_restriction = true

    http2_enabled             = true
    always_on                 = true
    use_32_bit_worker_process = false
    linux_fx_version          = "DOCKER|${var.container_registry_login_server}/${var.resource_prefix}:latest"

    cors {
      allowed_origins = [
        "https://${var.resource_prefix}public.z13.web.core.windows.net",
        "https://prime.cdc.gov",
        "https://${var.environment}.prime.cdc.gov",
        "https://reportstream.cdc.gov",
        "https://${var.environment}.reportstream.cdc.gov",
      ]
    }
  }

  app_settings = merge(local.all_app_settings, {
    "POSTGRES_URL" = "jdbc:postgresql://${var.resource_prefix}-pgsql.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require"
    # HHS Protect Storage Account
    "PartnerStorage" = var.primary_connection_string
  })

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    ignore_changes = [
      # Allows Docker versioning via GitHub Actions
      site_config[0].linux_fx_version,
      storage_account_access_key,
      tags,
      app_settings["APPINSIGHTS_INSTRUMENTATIONKEY"],
      app_settings["APPLICATIONINSIGHTS_CONNECTION_STRING"],
      app_settings["DOCKER_REGISTRY_SERVER_PASSWORD"],
      app_settings["POSTGRES_PASSWORD"],
      app_settings["POSTGRES_USER"],
      app_settings["PartnerStorage"],
      app_settings["AzureWebJobs.send.Disabled"],
      app_settings["AzureWebJobs.emailScheduleEngine.Disabled"]
    ]
  }
}

resource "azurerm_key_vault_access_policy" "slot_candidate_app_config_access_policy" {
  key_vault_id = var.app_config_key_vault_id
  tenant_id    = azurerm_function_app_slot.candidate.identity.0.tenant_id
  object_id    = azurerm_function_app_slot.candidate.identity.0.principal_id

  secret_permissions = [
    "Get",
  ]
}

resource "azurerm_key_vault_access_policy" "slot_candidate_client_config_access_policy" {
  key_vault_id = var.client_config_key_vault_id
  tenant_id    = azurerm_function_app_slot.candidate.identity.0.tenant_id
  object_id    = azurerm_function_app_slot.candidate.identity.0.principal_id

  secret_permissions = [
    "Get",
  ]
}

resource "azurerm_app_service_slot_virtual_network_swift_connection" "candidate_slot_vnet_integration" {
  slot_name      = azurerm_function_app_slot.candidate.name
  app_service_id = azurerm_function_app.function_app.id
  subnet_id      = var.use_cdc_managed_vnet ? var.subnets.public_subnets[0] : var.subnets.public_subnets[0]
}
