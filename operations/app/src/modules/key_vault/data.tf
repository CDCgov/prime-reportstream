data "azurerm_client_config" "current" {}

data "azurerm_subnet" "endpoint" {
  name                 = "endpoint"
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "endpoint_subnet" {
  name                 = "endpoint"
  virtual_network_name = "${var.resource_prefix}-East-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_key_vault_secret" "cyberark_ip_ingress" {
  name         = "cyberark-ip-ingress"
  key_vault_id = azurerm_key_vault.application.id
}