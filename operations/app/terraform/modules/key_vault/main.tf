resource "azurerm_key_vault_access_policy" "dev_access_policy" {
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  key_permissions = [
    "Get",
    "List",
    "Update",
    "Create",
    "Import",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  certificate_permissions = [
    "Get",
    "List",
    "Update",
    "Create",
    "Import",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
    "ManageContacts",
    "ManageIssuers",
    "GetIssuers",
    "ListIssuers",
    "SetIssuers",
    "DeleteIssuers",
  ]
}

resource "azurerm_key_vault_access_policy" "terraform_access_policy" {
  count        = var.environment == "dev" ? 0 : 1
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.terraform_object_id

  key_permissions = [
    "Create",
    "Get",
    "List",
    "Delete",
    "Purge"
  ]

  secret_permissions = [
    "Set",
    "List",
    "Get",
    "Delete",
    "Purge",
    "Recover"
  ]
}

module "application_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = data.azurerm_key_vault.application.id
  name           = data.azurerm_key_vault.application.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}

resource "azurerm_key_vault_access_policy" "dev_app_config_access_policy" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  key_permissions = []

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  certificate_permissions = []
}

resource "azurerm_key_vault_access_policy" "terraform_app_config_access_policy" {
  count        = var.environment == "dev" ? 0 : 1
  key_vault_id = data.azurerm_key_vault.app_config.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.terraform_object_id

  key_permissions = [
    "Create",
    "Get",
    "List",
    "Delete",
    "Purge"
  ]

  secret_permissions = [
    "Set",
    "List",
    "Get",
    "Delete",
    "Purge",
    "Recover"
  ]
}

module "app_config_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = data.azurerm_key_vault.app_config.id
  name           = data.azurerm_key_vault.app_config.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "client_config" {
  # Does not include "-keyvault" due to char limits (24)
  #checkov:skip=CKV_AZURE_110:Purge protection not needed for temporary environments
  #checkov:skip=CKV_AZURE_42:Recovery not needed for temporary environments
  name = var.client_config_kv_name

  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"

    ip_rules = var.terraform_caller_ip_address

    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      # Temp ignore ip_rules during tf development
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_client_config_access_policy" {
  key_vault_id = azurerm_key_vault.client_config.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  key_permissions = []

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  certificate_permissions = []
}

module "client_config_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.client_config.id
  name           = azurerm_key_vault.client_config.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}

resource "azurerm_key_vault_access_policy" "admin_functionapp_app_config" {
  key_vault_id = data.azurerm_key_vault.app_config.id
  tenant_id    = var.admin_function_app.identity[0].tenant_id
  object_id    = var.admin_function_app.identity[0].principal_id

  secret_permissions = ["Get", "List"]
}

resource "azurerm_key_vault_access_policy" "admin_functionapp_application" {
  key_vault_id = data.azurerm_key_vault.application.id
  tenant_id    = var.admin_function_app.identity[0].tenant_id
  object_id    = var.admin_function_app.identity[0].principal_id

  secret_permissions = ["List"]
}
