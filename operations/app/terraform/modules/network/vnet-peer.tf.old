resource "azurerm_virtual_network_peering" "vnet_peer_primary_to_remote" {
  for_each = setsubtract(toset(local.vnet_names), [local.vnet_primary_name])

  name                      = "${var.resource_prefix}-peer-${each.key}"
  resource_group_name       = var.resource_group
  virtual_network_name      = local.vnet_primary_name
  remote_virtual_network_id = data.azurerm_virtual_network.vnet[each.key].id
}

resource "azurerm_virtual_network_peering" "vnet_peer_remote_to_primary" {
  for_each = setsubtract(toset(local.vnet_names), [local.vnet_primary_name])

  name                      = "${var.resource_prefix}-peer-${local.vnet_primary_name}"
  resource_group_name       = var.resource_group
  virtual_network_name      = each.key
  remote_virtual_network_id = local.vnet_primary.id
}