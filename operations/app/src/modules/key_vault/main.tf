locals {
  frontdoor_object_id = "270e4d1a-12bd-4564-8a4b-c9de1bbdbe95"
  terraform_object_id = "4d81288c-27a3-4df8-b776-c9da8e688bc7"
}

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

    virtual_network_subnet_ids = []
  }

  lifecycle {
    prevent_destroy = true
  }

  tags = {
    "environment" = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "dev_access_policy" {
  key_vault_id = azurerm_key_vault.application.id
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

resource "azurerm_key_vault_access_policy" "frontdoor_access_policy" {
  key_vault_id = azurerm_key_vault.application.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = local.frontdoor_object_id

  secret_permissions = [
    "Get",
  ]
  certificate_permissions = [
    "Get",
  ]
}

resource "azurerm_key_vault_access_policy" "terraform_access_policy" {
  key_vault_id = azurerm_key_vault.application.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = local.terraform_object_id

  secret_permissions = [
    "Get",
  ]
  key_permissions = [
    "Get",
  ]
}

module "application_private_endpoint" {
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.application.id
  name               = azurerm_key_vault.application.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
  create_dns_record  = var.environment == "prod"
}

module "application_vault_private_endpoint" {
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.application.id
  name               = azurerm_key_vault.application.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint_subnet.id
  create_dns_record  = var.environment == "test"

  depends_on = [
    # Prevent unexpected order-of-operations by placing a hard dependency against the current private endpoint
    module.application_private_endpoint
  ]
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

    ip_rules = sensitive(concat(
      split(",", data.azurerm_key_vault_secret.cyberark_ip_ingress.value),
      [var.terraform_caller_ip_address],
    ))

    virtual_network_subnet_ids = []
  }

  lifecycle {
    prevent_destroy = true
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
  key_vault_id = azurerm_key_vault.app_config.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = local.terraform_object_id

  secret_permissions = [
    "Get",
  ]
}

module "app_config_private_endpoint" {
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.app_config.id
  name               = azurerm_key_vault.app_config.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
  create_dns_record  = var.environment == "prod"
}

module "app_config_vault_private_endpoint" {
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.app_config.id
  name               = azurerm_key_vault.app_config.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint_subnet.id
  create_dns_record  = var.environment == "test"

  depends_on = [
    # Prevent unexpected order-of-operations by placing a hard dependency against the current private endpoint
    module.app_config_private_endpoint
  ]
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

    ip_rules = sensitive(concat(
      split(",", data.azurerm_key_vault_secret.cyberark_ip_ingress.value),
      [var.terraform_caller_ip_address],
    ))

    virtual_network_subnet_ids = []
  }

  lifecycle {
    prevent_destroy = true
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
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.client_config.id
  name               = azurerm_key_vault.client_config.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint.id
  create_dns_record  = var.environment == "prod"
}

module "client_config_vault_private_endpoint" {
  source             = "../common/private_endpoint"
  resource_id        = azurerm_key_vault.client_config.id
  name               = azurerm_key_vault.client_config.name
  type               = "key_vault"
  resource_group     = var.resource_group
  location           = var.location
  endpoint_subnet_id = data.azurerm_subnet.endpoint_subnet.id
  create_dns_record  = var.environment == "test"

  depends_on = [
    # Prevent unexpected order-of-operations by placing a hard dependency against the current private endpoint
    module.client_config_private_endpoint
  ]
}
