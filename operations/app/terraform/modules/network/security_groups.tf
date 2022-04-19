/* Network security groups */
data "azurerm_network_security_group" "vnet_nsg_public" {
  for_each            = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].network_security_groups, "public") }
  name                = "${var.resource_prefix}-${var.azure_vns[each.key].nsg_prefix}nsg.public"
  resource_group_name = var.resource_group
}

data "azurerm_network_security_group" "vnet_nsg_private" {
  for_each            = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].network_security_groups, "private") }
  name                = "${var.resource_prefix}-${var.azure_vns[each.key].nsg_prefix}nsg.private"
  resource_group_name = var.resource_group
}

