terraform {
    required_version = ">= 0.14"
}

resource "azurerm_function_app" "function_app" {
  name = "${var.resource_prefix}-functionapp"
  location = var.location
  resource_group_name = var.resource_group
  app_service_plan_id = var.app_service_plan_id
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
      name = "Ron IP"
      priority = 120
      ip_address = "165.225.48.87/31" # /31 is correct, can be 165.225.48.87 or 165.225.48.88
    }

    ip_restriction {
      action = "Allow"
      name = "Jim IP"
      priority = 130
      ip_address = "108.51.58.151/32"
    }

    scm_use_main_ip_restriction = true

    http2_enabled = true
    always_on = true
    use_32_bit_worker_process = false
    linux_fx_version = "DOCKER|${var.login_server}/${var.resource_prefix}:latest"

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

  app_settings = {
    "POSTGRES_USER" = var.postgres_user
    "POSTGRES_PASSWORD" = var.postgres_password
    "POSTGRES_URL" = var.postgres_url

    "PRIME_ENVIRONMENT" = (var.environment == "prod" ? "prod" : "test")

    "OKTA_baseUrl" = "hhs-prime.okta.com"
    "OKTA_redirect" = var.okta_redirect_url

    # Manage client secrets via a Key Vault
    "CREDENTIAL_STORAGE_METHOD" ="AZURE"
    "CREDENTIAL_KEY_VAULT_NAME" = "${var.resource_prefix}-clientconfig"

    # Manage app secrets via a Key Vault
    "SECRET_STORAGE_METHOD" = "AZURE"
    "SECRET_KEY_VAULT_NAME" = "${var.resource_prefix}-appconfig"

    # Route outbound traffic through the VNET
    "WEBSITE_VNET_ROUTE_ALL" = 1

    # Route storage account access through the VNET
    "WEBSITE_CONTENTOVERVNET" = 1

    # Use the VNET DNS server (so we receive private endpoint URLs
    "WEBSITE_DNS_SERVER" = "168.63.129.16"

    # HHS Protect Storage Account
    "PartnerStorage" = var.storage_partner_connection_string

    "DOCKER_REGISTRY_SERVER_URL" = var.login_server
    "DOCKER_REGISTRY_SERVER_USERNAME" = var.admin_user
    "DOCKER_REGISTRY_SERVER_PASSWORD" = var.admin_password
    "DOCKER_CUSTOM_IMAGE_NAME" = "${var.login_server}/${var.resource_prefix}:latest"

    "WEBSITES_ENABLE_APP_SERVICE_STORAGE" = false

    "APPINSIGHTS_INSTRUMENTATIONKEY" = var.ai_instrumentation_key

    "FEATURE_FLAG_SETTINGS_ENABLED" = true
  }

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }
}

// DISABLED AS FRONT DOOR CAN NOT CONNECT - RKH

//module "function_app_private_endpoint" {
//  source = "../common/private_endpoint"
//  resource_id = azurerm_function_app.function_app.id
//  name = azurerm_function_app.function_app.name
//  type = "function_app"
//  resource_group = var.resource_group
//  location = var.location
//  endpoint_subnet_id = var.endpoint_subnet_id
//}

resource "azurerm_key_vault_access_policy" "functionapp_app_config_access_policy" {
  # This is a hack. The function_app module has a bug where it does not export the values until after being updated.
  # By using a count, we workout the bug by running two deploy. The first deploy created the system-assigned identity.
  # The second deploy adds the Key Value access policy.
  count = azurerm_function_app.function_app.identity.0.tenant_id != null ? 1 : 0

  key_vault_id = var.app_config_key_vault_id
  tenant_id = azurerm_function_app.function_app.identity.0.tenant_id
  object_id = azurerm_function_app.function_app.identity.0.principal_id

  secret_permissions = [ "Get" ]
}

resource "azurerm_key_vault_access_policy" "functionapp_client_config_access_policy" {
  # This is a hack. The function_app module has a bug where it does not export the values until after being updated.
  # By using a count, we workout the bug by running two deploy. The first deploy created the system-assigned identity.
  # The second deploy adds the Key Value access policy.
  count = azurerm_function_app.function_app.identity.0.tenant_id != null ? 1 : 0

  key_vault_id = var.client_config_key_vault_id
  tenant_id = azurerm_function_app.function_app.identity.0.tenant_id
  object_id = azurerm_function_app.function_app.identity.0.principal_id

  secret_permissions = [ "Get" ]
}

resource "azurerm_app_service_virtual_network_swift_connection" "function_app_vnet_integration" {
  app_service_id = azurerm_function_app.function_app.id
  subnet_id = var.public_subnet_id
}
