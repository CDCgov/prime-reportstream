resource "azurerm_private_dns_zone" "default" {
  name                = var.dns_name
  resource_group_name = var.common.resource_group.name
}

resource "azurerm_private_dns_zone_virtual_network_link" "default" {
  for_each = var.dns_links

  name                  = "vnet-link-${each.value.vnet_key}-${var.key}"
  resource_group_name   = var.common.resource_group.name
  private_dns_zone_name = azurerm_private_dns_zone.default.name
  virtual_network_id    = lookup(var.vnets, "vnet-${each.value.vnet_key}", "")
  registration_enabled  = each.value.registration_enabled
}
