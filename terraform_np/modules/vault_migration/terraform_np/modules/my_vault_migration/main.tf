
resource "azurerm_key_vault" "pdhdemo1_appconfigs5m" {
  name                            = var.pdhdemo1_appconfigs5m_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo1_appconfigs5m" {
  key_vault_id = azurerm_key_vault.pdhdemo1_appconfigs5m.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo1_appconfigs5m_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo1_appconfigs5m.id
  name           = azurerm_key_vault.pdhdemo1_appconfigs5m.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo1_clientconfigs5m" {
  name                            = var.pdhdemo1_clientconfigs5m_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo1_clientconfigs5m" {
  key_vault_id = azurerm_key_vault.pdhdemo1_clientconfigs5m.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo1_clientconfigs5m_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo1_clientconfigs5m.id
  name           = azurerm_key_vault.pdhdemo1_clientconfigs5m.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo1_keyvaults5m" {
  name                            = var.pdhdemo1_keyvaults5m_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo1_keyvaults5m" {
  key_vault_id = azurerm_key_vault.pdhdemo1_keyvaults5m.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo1_keyvaults5m_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo1_keyvaults5m.id
  name           = azurerm_key_vault.pdhdemo1_keyvaults5m.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo2_appconfig1wu" {
  name                            = var.pdhdemo2_appconfig1wu_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo2_appconfig1wu" {
  key_vault_id = azurerm_key_vault.pdhdemo2_appconfig1wu.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo2_appconfig1wu_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo2_appconfig1wu.id
  name           = azurerm_key_vault.pdhdemo2_appconfig1wu.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo2_clientconfig1wu" {
  name                            = var.pdhdemo2_clientconfig1wu_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo2_clientconfig1wu" {
  key_vault_id = azurerm_key_vault.pdhdemo2_clientconfig1wu.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo2_clientconfig1wu_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo2_clientconfig1wu.id
  name           = azurerm_key_vault.pdhdemo2_clientconfig1wu.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo2_keyvault1wu" {
  name                            = var.pdhdemo2_keyvault1wu_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo2_keyvault1wu" {
  key_vault_id = azurerm_key_vault.pdhdemo2_keyvault1wu.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo2_keyvault1wu_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo2_keyvault1wu.id
  name           = azurerm_key_vault.pdhdemo2_keyvault1wu.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo3_appconfigjeo" {
  name                            = var.pdhdemo3_appconfigjeo_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo3_appconfigjeo" {
  key_vault_id = azurerm_key_vault.pdhdemo3_appconfigjeo.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo3_appconfigjeo_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo3_appconfigjeo.id
  name           = azurerm_key_vault.pdhdemo3_appconfigjeo.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo3_clientconfigjeo" {
  name                            = var.pdhdemo3_clientconfigjeo_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo3_clientconfigjeo" {
  key_vault_id = azurerm_key_vault.pdhdemo3_clientconfigjeo.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo3_clientconfigjeo_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo3_clientconfigjeo.id
  name           = azurerm_key_vault.pdhdemo3_clientconfigjeo.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}


resource "azurerm_key_vault" "pdhdemo3_keyvaultjeo" {
  name                            = var.pdhdemo3_keyvaultjeo_name
  location                        = var.location
  resource_group_name             = var.prod_resource_group
  sku_name                        = "premium"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  enabled_for_deployment          = true
  enabled_for_disk_encryption     = true
  enabled_for_template_deployment = true
  purge_protection_enabled        = var.is_temp_env == true ? false : true

  network_acls {
    bypass                     = "AzureServices"
    default_action             = "Deny"
    ip_rules                   = var.terraform_caller_ip_address
    virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  lifecycle {
    prevent_destroy = false
    ignore_changes = [
      network_acls[0].ip_rules
    ]
  }

  tags = {
    environment = var.environment
  }
}

resource "azurerm_key_vault_access_policy" "admin_access_pdhdemo3_keyvaultjeo" {
  key_vault_id = azurerm_key_vault.pdhdemo3_keyvaultjeo.id
  tenant_id    = data.azurerm_client_config.current.tenant_id
  object_id    = var.aad_object_keyvault_admin

  secret_permissions = [
    "Get",
    "List",
    "Set",
    "Delete",
    "Recover",
    "Backup",
    "Restore",
  ]

  key_permissions         = []
  certificate_permissions = []
}

module "pdhdemo3_keyvaultjeo_private_endpoint" {
  for_each = var.subnets.primary_endpoint_subnets

  source         = "../common/private_endpoint"
  resource_id    = azurerm_key_vault.pdhdemo3_keyvaultjeo.id
  name           = azurerm_key_vault.pdhdemo3_keyvaultjeo.name
  type           = "key_vault"
  resource_group = var.prod_resource_group
  location       = var.location

  endpoint_subnet_ids = each.value
  dns_vnet            = "vnet"
  resource_prefix     = var.resource_prefix
  dns_zone            = var.dns_zones["vaultcore"].name
}

