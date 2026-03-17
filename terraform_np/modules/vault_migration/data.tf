data "azurerm_client_config" "current" {}

# Demo1 Vaults
data "azurerm_key_vault" "pdhdemo1_appconfigs5m" {
  name                = var.pdhdemo1_appconfigs5m_name
  resource_group_name = var.pdhdemo1_appconfigs5m_rg
}

data "azurerm_key_vault" "pdhdemo1_clientconfigs5m" {
  name                = var.pdhdemo1_clientconfigs5m_name
  resource_group_name = var.pdhdemo1_clientconfigs5m_rg
}

data "azurerm_key_vault" "pdhdemo1_keyvaults5m" {
  name                = var.pdhdemo1_keyvaults5m_name
  resource_group_name = var.pdhdemo1_keyvaults5m_rg
}

# Demo2 Vaults
data "azurerm_key_vault" "pdhdemo2_appconfig1wu" {
  name                = var.pdhdemo2_appconfig1wu_name
  resource_group_name = var.pdhdemo2_appconfig1wu_rg
}

data "azurerm_key_vault" "pdhdemo2_clientconfig1wu" {
  name                = var.pdhdemo2_clientconfig1wu_name
  resource_group_name = var.pdhdemo2_clientconfig1wu_rg
}

data "azurerm_key_vault" "pdhdemo2_keyvault1wu" {
  name                = var.pdhdemo2_keyvault1wu_name
  resource_group_name = var.pdhdemo2_keyvault1wu_rg
}

# Demo3 Vaults
data "azurerm_key_vault" "pdhdemo3_appconfigjeo" {
  name                = var.pdhdemo3_appconfigjeo_name
  resource_group_name = var.pdhdemo3_appconfigjeo_rg
}

data "azurerm_key_vault" "pdhdemo3_clientconfigjeo" {
  name                = var.pdhdemo3_clientconfigjeo_name
  resource_group_name = var.pdhdemo3_clientconfigjeo_rg
}

data "azurerm_key_vault" "pdhdemo3_keyvaultjeo" {
  name                = var.pdhdemo3_keyvaultjeo_name
  resource_group_name = var.pdhdemo3_keyvaultjeo_rg
}

