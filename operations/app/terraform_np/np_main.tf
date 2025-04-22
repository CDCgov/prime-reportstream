module "pdhprod_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhprod-serviceplan"
  resource_group_name = "ddphss-prim-prd-moderate-app-vnet"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "prod"
    managed-by  = "terraform"
  }
}

module "pdhstaging_serviceplan" {
  source              = "./modules/app_service_plan"
  name                = "pdhstaging-serviceplan"
  resource_group_name = "ddphss-prim-stg-moderate-app-vnet"
  location            = "East US"
  kind                = "Linux"
  sku_tier            = "PremiumV2"
  sku_size            = "P3v2"
  reserved            = true
  per_site_scaling    = false
  tags = {
    environment = "staging"
    managed-by  = "terraform"
  }
}
resource "azurerm_key_vault" "pdhprod_vault" {
  name                        = "pdhprod-admin-functionapp"
  location                    = "East US"
  resource_group_name         = "ddphss-prim-prd-moderate-app-vnet"
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  sku_name                    = "standard"
  purge_protection_enabled    = true
  soft_delete_enabled         = true
  enabled_for_deployment      = false
  enabled_for_disk_encryption = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prod"
    managed-by  = "terraform"
  }
}
resource "azurerm_key_vault" "pdhprod_appconfig" {
  name                        = "pdhprod-appconfig"
  location                    = "East US"
  resource_group_name         = "ddphss-prim-prd-moderate-app-vnet"
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  sku_name                    = "premium"
  purge_protection_enabled    = true
  soft_delete_enabled         = true
  enabled_for_deployment      = false
  enabled_for_disk_encryption = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prod"
    managed-by  = "terraform"
  }
}

resource "azurerm_key_vault" "pdhprod_clientconfig" {
  name                        = "pdhprod-clientconfig"
  location                    = "East US"
  resource_group_name         = "ddphss-prim-prd-moderate-app-vnet"
  tenant_id                   = data.azurerm_client_config.current.tenant_id
  sku_name                    = "premium"
  purge_protection_enabled    = true
  soft_delete_enabled         = true
  enabled_for_deployment      = false
  enabled_for_disk_encryption = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prod"
    managed-by  = "terraform"
  }
}

