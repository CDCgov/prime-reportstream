# resource "azurerm_virtual_network" "west" {
#   name                = "${var.resource_prefix}-West-vnet"
#   location            = "West US"
#   resource_group_name = var.resource_group
#   address_space       = var.west_address_space
#   dns_servers         = var.west_dns_servers

# }

# resource "azurerm_virtual_network" "east" {
#   name                = "${var.resource_prefix}-East-vnet"
#   location            = "East US"
#   resource_group_name = var.resource_group
#   address_space       = var.east_address_space
#   dns_servers         = var.east_dns_servers
# }

# resource "azurerm_virtual_network" "vnet" {
#   name                = "${var.resource_prefix}-vnet"
#   location            = "East US"
#   resource_group_name = var.resource_group
#   address_space       = var.vnet_address_space
#   tags = {
#     "environment" = var.environment
#   }
# }

# resource "azurerm_virtual_network" "vnet_peer" {
#   name                = "${var.resource_prefix}-vnet-peer"
#   location            = "West US"
#   resource_group_name = var.resource_group
#   address_space       = var.vnet_peer_address_space
#   tags = {
#     "environment" = var.environment
#   }
# }

resource "azurerm_virtual_network" "azure_vns" {
  for_each = var.azure_vns
  name                = "${var.resource_prefix}-${each.key}"
  location            = each.value.location
  resource_group_name = var.resource_group
  address_space       = [each.value.address_space]
  # tags = {
  #   "environment" = var.environment
  # }
}