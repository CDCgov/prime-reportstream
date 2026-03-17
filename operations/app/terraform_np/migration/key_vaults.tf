resource "azurerm_key_vault" "ddphss_prd_vault" {
  name                            = "ddphss-prd-admin-functionapp"
  location                        = "East US"
  resource_group_name             = "ddphss-prim-prd-moderate-app-rg"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  sku_name                        = "standard"
  purge_protection_enabled        = true
  soft_delete_enabled             = true
  enabled_for_deployment          = false
  enabled_for_disk_encryption     = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prd"
    managed-by  = "terraform"
  }
}
resource "azurerm_key_vault" "ddphss_prd_appconfig" {
  name                            = "ddphss-prd-appconfig"
  location                        = "East US"
  resource_group_name             = "ddphss-prim-prd-moderate-app-rg"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  sku_name                        = "premium"
  purge_protection_enabled        = true
  soft_delete_enabled             = true
  enabled_for_deployment          = false
  enabled_for_disk_encryption     = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prd"
    managed-by  = "terraform"
  }
}

resource "azurerm_key_vault" "ddphss_prd_clientconfig" {
  name                            = "ddphss-prd-clientconfig"
  location                        = "East US"
  resource_group_name             = "ddphss-prim-prd-moderate-app-rg"
  tenant_id                       = data.azurerm_client_config.current.tenant_id
  sku_name                        = "premium"
  purge_protection_enabled        = true
  soft_delete_enabled             = true
  enabled_for_deployment          = false
  enabled_for_disk_encryption     = false
  enabled_for_template_deployment = false

  tags = {
    environment = "prd"
    managed-by  = "terraform"
  }
}

