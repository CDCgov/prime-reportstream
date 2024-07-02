output "primary_vnet_id" {
  value = data.azurerm_virtual_network.vnet["app-vnet"].id
}

output "subnets" {
  value = local.subnets
}

output "dns_zones" {
  value = azurerm_private_dns_zone.zone
}
