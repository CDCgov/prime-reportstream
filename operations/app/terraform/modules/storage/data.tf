# data "azurerm_client_config" "current" {}

# // Network

# data "azurerm_subnet" "public" {
#   name                 = "public"
#   virtual_network_name = "${var.resource_prefix}-vnet"
#   resource_group_name  = var.resource_group
# }

# data "azurerm_subnet" "container" {
#   name                 = "container"
#   virtual_network_name = "${var.resource_prefix}-vnet"
#   resource_group_name  = var.resource_group
# }

# data "azurerm_subnet" "endpoint" {
#   name                 = "endpoint"
#   virtual_network_name = "${var.resource_prefix}-vnet"
#   resource_group_name  = var.resource_group
# }

# data "azurerm_subnet" "public_subnet" {
#   name                 = "public"
#   virtual_network_name = "${var.resource_prefix}-East-vnet"
#   resource_group_name  = var.resource_group
# }

# data "azurerm_subnet" "container_subnet" {
#   name                 = "container"
#   virtual_network_name = "${var.resource_prefix}-East-vnet"
#   resource_group_name  = var.resource_group
# }

# data "azurerm_subnet" "endpoint_subnet" {
#   name                 = "endpoint"
#   virtual_network_name = "${var.resource_prefix}-East-vnet"
#   resource_group_name  = var.resource_group
# }


# // Key Vault

# data "azurerm_key_vault" "application" {
#   name                = "${var.resource_prefix}-keyvault"
#   resource_group_name = var.resource_group
# }

# data "azurerm_key_vault_secret" "hhsprotect_ip_ingress" {
#   name         = "hhsprotect-ip-ingress"
#   key_vault_id = data.azurerm_key_vault.application.id
# }

# data "azurerm_key_vault_secret" "cyberark_ip_ingress" {
#   name         = "cyberark-ip-ingress"
#   key_vault_id = data.azurerm_key_vault.application.id
# }