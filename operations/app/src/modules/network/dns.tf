// This resource exists once per resource group
resource "azurerm_private_dns_zone" "dns_zone_private" {
  for_each = toset(local.dns_zones_private)

  name                = each.value
  resource_group_name = var.resource_group
}

// This resource exists once per vnet
module "vnet_dns" {
  for_each = data.azurerm_virtual_network.vnet
  source   = "../common/vnet_dns_zones"

  resource_prefix = var.resource_prefix
  resource_group  = var.resource_group
  dns_zone_names  = local.dns_zones_private
  vnet            = each.value
}