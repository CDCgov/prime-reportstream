locals {
  # These object ids correspond to developers with access
  # to key vault
  dev_object_ids = [
    # Richard Teasley
    "34232fe8-00ad-4bd0-9afb-eb9b3cc93ffe",
    # IAMB-Prod-KV
    "cd341fbc-26a3-405c-a350-c4237a27aa93",
    # Ron Heft
    "637fb7df-c200-4e0d-ba86-608576acb786",
    # Maurice Reeves
    "414537da-0ba5-4db1-93f6-dd828e9a480a",
    # Jim Duff
    "24669a80-a2d3-425a-8c80-92e05ea8341f",
    # Rick Hawes
    "96c8ed83-c3df-4a04-9dfe-7cd8487e342e"
  ]

  frontdoor_object_id = "270e4d1a-12bd-4564-8a4b-c9de1bbdbe95"
}

resource "azurerm_key_vault" "application" {
  name = "${var.resource_prefix}-keyvault"
  location = var.location
  resource_group_name = var.resource_group
  sku_name = "premium"
  tenant_id = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment = true
  enabled_for_disk_encryption = true
  enabled_for_template_deployment = true
  purge_protection_enabled = true

  network_acls {
    bypass = "AzureServices"
    default_action = "Deny"
    virtual_network_subnet_ids = [] // We're using a private endpoint, so none need to be associated
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_access_policy" {
  count = length(local.dev_object_ids)
  key_vault_id = azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = local.dev_object_ids[count.index]

  key_permissions = [
    "Get",
    "List",
    "Update",
    "Create",
    "Import",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
  ]

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
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
    "DeleteIssuers" 
  ]
}

resource "azurerm_key_vault_access_policy" "frontdoor_access_policy" {
  key_vault_id = azurerm_key_vault.application.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = local.frontdoor_object_id

  secret_permissions = [ "Get" ]
  certificate_permissions = [ "Get" ]
}

module "application_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_key_vault.application.id
  name = azurerm_key_vault.application.name
  type = "key_vault"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

resource "azurerm_key_vault" "app_config" {
  name = "${var.resource_prefix}-appconfig" # Does not include "-keyvault" due to char limits (24)
  location = var.location
  resource_group_name = var.resource_group
  sku_name = "premium"
  tenant_id = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment = true
  enabled_for_disk_encryption = true
  enabled_for_template_deployment = true
  purge_protection_enabled = true

  network_acls {
    bypass = "AzureServices"
    default_action = "Deny"
    virtual_network_subnet_ids = [] // We're using a private endpoint, so none need to be associated
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_app_config_access_policy" {
  count = length(local.dev_object_ids)
  key_vault_id = azurerm_key_vault.app_config.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = local.dev_object_ids[count.index]

  key_permissions = []

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
  ]

  certificate_permissions = []
}

module "app_config_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_key_vault.app_config.id
  name = azurerm_key_vault.app_config.name
  type = "key_vault"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}

resource "azurerm_key_vault" "client_config" {
  name = "${var.resource_prefix}-clientconfig" # Does not include "-keyvault" due to char limits (24)
  location = var.location
  resource_group_name = var.resource_group
  sku_name = "premium"
  tenant_id = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment = true
  enabled_for_disk_encryption = true
  enabled_for_template_deployment = true
  purge_protection_enabled = true

  network_acls {
    bypass = "AzureServices"
    default_action = "Deny"
    virtual_network_subnet_ids = [] // We're using a private endpoint, so none need to be associated
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_client_config_access_policy" {
  count = length(local.dev_object_ids)
  key_vault_id = azurerm_key_vault.client_config.id
  tenant_id = data.azurerm_client_config.current.tenant_id
  object_id = local.dev_object_ids[count.index]

  key_permissions = []

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore"
  ]

  certificate_permissions = []
}

module "client_config_private_endpoint" {
  source = "../common/private_endpoint"
  resource_id = azurerm_key_vault.client_config.id
  name = azurerm_key_vault.client_config.name
  type = "key_vault"
  resource_group = var.resource_group
  location = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
}
