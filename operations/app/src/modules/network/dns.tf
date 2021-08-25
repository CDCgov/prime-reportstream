resource "azurerm_private_dns_zone" "dns_zone_private" {
  for_each = toset(local.dns_zones_private)

  name                = each.value
  resource_group_name = var.resource_group
}

resource "azurerm_private_dns_zone_virtual_network_link" "dns_zone_private_link_primary" {
  for_each = azurerm_private_dns_zone.dns_zone_private

  name                  = "${var.resource_prefix}-vnet-primary-${each.value.name}"
  private_dns_zone_name = each.value.name
  resource_group_name   = var.resource_group
  virtual_network_id    = local.vnet_primary.id
}

resource "azurerm_private_dns_zone_virtual_network_link" "dns_zone_private_link_secondary" {
  for_each = azurerm_private_dns_zone.dns_zone_private

  name                  = "${var.resource_prefix}-vnet-secondary-${each.value.name}"
  private_dns_zone_name = each.value.name
  resource_group_name   = var.resource_group
  virtual_network_id    = data.azurerm_virtual_network.vnet[local.vnet_names[1]].id
}