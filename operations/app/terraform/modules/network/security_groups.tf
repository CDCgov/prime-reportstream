/* Network security groups */
# resource "azurerm_network_security_group" "vnet_nsg_public" {
#   for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].network_security_groups, "public")}

#   name                = "${var.resource_prefix}-${var.azure_vns[each.value].nsg_prefix}nsg.public"
#   location            = var.azure_vns[each.value].location
#   resource_group_name = var.resource_group
# }

# resource "azurerm_network_security_group" "vnet_nsg_private" {
#   count = length(var.network_security_groups["private"])

#   name                = "${var.resource_prefix}-${var.azure_vns[var.network_security_groups["private"][count.index]].nsg_prefix}nsg.private"
#   location            = var.azure_vns[var.network_security_groups["private"][count.index]].location
#   resource_group_name = var.resource_group
# }

# resource "azurerm_subnet_network_security_group_association" "public_to_nsg_public" {
#    count = length(azurerm_subnet.public_subnet)

#   subnet_id                 = azurerm_subnet.public_subnet[count.index].id
#   network_security_group_id = azurerm_network_security_group.vnet_nsg_public[count.index].id
# }

# resource "azurerm_subnet_network_security_group_association" "container_to_nsg_public" {
#   count = length(azurerm_subnet.container_subnet)

#   subnet_id                 = azurerm_subnet.container_subnet[count.index].id
#   network_security_group_id = azurerm_network_security_group.vnet_nsg_public[count.index].id
# }

# resource "azurerm_subnet_network_security_group_association" "private_to_nsg_private" {
#   count = length(azurerm_subnet.private_subnet)

#   subnet_id                 = azurerm_subnet.private_subnet[count.index].id
#   network_security_group_id = azurerm_network_security_group.vnet_nsg_private[count.index].id
# }

# resource "azurerm_subnet_network_security_group_association" "endpoint_to_nsg_private" {
#   count = length(azurerm_subnet.endpoint_subnet)

#   subnet_id                 = azurerm_subnet.endpoint_subnet[count.index].id
#   network_security_group_id = azurerm_network_security_group.vnet_nsg_private[count.index].id
# }