resource "azurerm_virtual_network_peering" "vnet_peer" {
  for_each = setsubtract(toset(local.vnet_names), [local.vnet_primary_name])

  name                      = "${var.resource_prefix}-peer-${each.key}"
  resource_group_name       = var.resource_group
  virtual_network_name      = local.vnet_primary_name
  remote_virtual_network_id = data.azurerm_virtual_network.vnet[each.key].id
}