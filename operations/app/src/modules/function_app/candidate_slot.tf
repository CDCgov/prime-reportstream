locals {
  slots = {
    active: azurerm_function_app.function_app
    candidate: azurerm_function_app_slot.candidate
  }
}

resource "azurerm_function_app_slot" "candidate" {
  function_app_name          = azurerm_function_app.function_app.name
  name                       = "candidate"
  location                   = var.location
  resource_group_name        = var.resource_group
  app_service_plan_id        = data.azurerm_app_service_plan.service_plan.id
  storage_account_name       = data.azurerm_storage_account.storage_account_candidate.name
  storage_account_access_key = data.azurerm_storage_account.storage_account_candidate.primary_access_key
  https_only                 = true
  os_type                    = "linux"
  version                    = "~3"
  enable_builtin_logging     = false

  site_config {
    ip_restriction {
      action                    = "Allow"
      name                      = "AllowVNetTraffic"
      priority                  = 100
      virtual_network_subnet_id = data.azurerm_subnet.public.id
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
    linux_fx_version          = "DOCKER|${data.azurerm_container_registry.container_registry.login_server}/${var.resource_prefix}:latest"

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
    "POSTGRES_URL" = "jdbc:postgresql://${data.azurerm_postgresql_server.postgres_server.name}.postgres.database.azure.com:5432/prime_data_hub_candidate?sslmode=require"

    # HHS Protect Storage Account
    "PartnerStorage" = data.azurerm_storage_account.storage_partner_candidate.primary_connection_string
  })

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }

  lifecycle {
    ignore_changes = [site_config[0].linux_fx_version] # Allows Docker versioning via GitHub Actions
  }
}

resource "azurerm_key_vault_access_policy" "slot_candidate_app_config_access_policy" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  tenant_id    = azurerm_function_app_slot.candidate.identity.0.tenant_id
  object_id    = azurerm_function_app_slot.candidate.identity.0.principal_id

  secret_permissions = [
    "Get"]
}

resource "azurerm_key_vault_access_policy" "slot_candidate_client_config_access_policy" {
  key_vault_id = data.azurerm_key_vault.client_config.id
  tenant_id    = azurerm_function_app_slot.candidate.identity.0.tenant_id
  object_id    = azurerm_function_app_slot.candidate.identity.0.principal_id

  secret_permissions = [
    "Get"]
}

resource "azurerm_app_service_slot_virtual_network_swift_connection" "candidate_slot_vnet_integration" {
  slot_name      = azurerm_function_app_slot.candidate.name
  app_service_id = azurerm_function_app.function_app.id
  subnet_id      = data.azurerm_subnet.public.id
}