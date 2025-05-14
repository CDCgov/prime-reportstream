resource "azurerm_private_dns_zone_virtual_network_link" "dns_zone_private_link" {
  for_each = toset(var.dns_zone_names)

  name                  = "${var.resource_prefix}-${var.vnet.name}-${each.key}"
  private_dns_zone_name = each.key
  resource_group_name   = var.resource_group
  virtual_network_id    = var.vnet.id
}