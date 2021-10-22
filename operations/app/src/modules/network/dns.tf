// This resource exists once per resource group
resource "azurerm_private_dns_zone" "dns_zone_private" {
  for_each = toset(local.dns_zones_private)

  name                = each.value
  resource_group_name = var.resource_group
}

// This resource exists once per vnet
module "vnet_dns" {
  for_each = toset(local.vnet_names)
  source   = "../common/vnet_dns_zones"

  resource_prefix = var.resource_prefix
  resource_group  = var.resource_group
  dns_zone_names  = setsubtract(local.dns_zones_private, local.omit_dns_zones_private_in_cdc_vnet)
  vnet            = data.azurerm_virtual_network.vnet[each.value]
}