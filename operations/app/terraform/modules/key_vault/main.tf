data "azurerm_client_config" "current" {}

resource "azurerm_key_vault" "application" {
  name                            = "${var.resource_prefix}-keyvault"
  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = true

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"

    ip_rules = sensitive(concat(
      [var.terraform_caller_ip_address],
    ))

    virtual_network_subnet_ids = concat(var.public_subnet, var.container_subnet, var.endpoint_subnet)
  }

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    "environment" = var.environment
  }
}

# resource "azurerm_key_vault_access_policy" "dev_access_policy" {
#   key_vault_id = azurerm_key_vault.application.id
#   tenant_id    = data.azurerm_client_config.current.tenant_id
#   object_id    = var.aad_object_keyvault_admin

#   key_permissions = [
#     "Get",
#     "List",
#     "Update",
#     "Create",
#     "Import",
#     "Delete",
#     "Recover",
#     "Backup",
#     "Restore",
#   ]

#   secret_permissions = [
#     "Get",
#     "List",
#     "Set",
#     "Delete",
#     "Recover",
#     "Backup",
#     "Restore",
#   ]

#   certificate_permissions = [
#     "Get",
#     "List",
#     "Update",
#     "Create",
#     "Import",
#     "Delete",
#     "Recover",
#     "Backup",
#     "Restore",
#     "ManageContacts",
#     "ManageIssuers",
#     "GetIssuers",
#     "ListIssuers",
#     "SetIssuers",
#     "DeleteIssuers",
#   ]
# }

# resource "azurerm_key_vault_access_policy" "frontdoor_access_policy" {
#   key_vault_id = azurerm_key_vault.application.id
#   tenant_id    = data.azurerm_client_config.current.tenant_id
#   object_id    = local.frontdoor_object_id

#   secret_permissions = [
#     "Get",
#   ]
#   certificate_permissions = [
#     "Get",
#   ]
# }

resource "azurerm_key_vault_access_policy" "terraform_access_policy" {
  count = var.environment == "dev" ?  0 : 1
  key_vault_id = azurerm_key_vault.application.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.terraform_object_id

  secret_permissions = [
    "Get",
  ]
  key_permissions = [
    "Get",
  ]
}

module "application_private_endpoint" {
  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.application.id
  name           = azurerm_key_vault.application.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = [var.endpoint_subnet[0]]

  endpoint_subnet_id_for_dns = var.endpoint_subnet[0]
}

resource "azurerm_key_vault" "app_config" {
  name = "${var.resource_prefix}-appconfig"
  # Does not include "-keyvault" due to char limits (24)
  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = true

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"

    ip_rules = [var.terraform_caller_ip_address]

    virtual_network_subnet_ids = concat(var.public_subnet, var.container_subnet, var.endpoint_subnet)
  }

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_app_config_access_policy" {
  key_vault_id = azurerm_key_vault.app_config.id
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
  count = var.environment == "dev" ?  0 : 1
  key_vault_id = azurerm_key_vault.app_config.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.terraform_object_id

  secret_permissions = [
    "Get",
  ]
}

module "app_config_private_endpoint" {
  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.app_config.id
  name           = azurerm_key_vault.app_config.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = var.endpoint_subnet

  endpoint_subnet_id_for_dns = var.endpoint_subnet[0]
}


resource "azurerm_key_vault" "client_config" {
  # Does not include "-keyvault" due to char limits (24)
  name = "${var.resource_prefix}-clientconfig"

  location                        = var.location
  resource_group_name             = var.resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = true

  network_acls {
    bypass         = "AzureServices"
    default_action = "Deny"

    ip_rules = [var.terraform_caller_ip_address]

    virtual_network_subnet_ids = concat(var.public_subnet, var.container_subnet, var.endpoint_subnet)
  }

  lifecycle {
    prevent_destroy = false
  }

  tags = {
    "environment" = var.environment
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
  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.client_config.id
  name           = azurerm_key_vault.client_config.name
  type           = "key_vault"
  resource_group = var.resource_group
  location       = var.location

  endpoint_subnet_ids = var.endpoint_subnet

  endpoint_subnet_id_for_dns = var.endpoint_subnet[0]
}
