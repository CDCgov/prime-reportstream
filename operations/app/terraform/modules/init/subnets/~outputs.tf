output "dns_gateway_subnet_id" {
  value = var.vnet == "${var.resource_prefix}-vnet" ? azurerm_subnet.init["GatewaySubnet"].id : ""
}

output "dns_container_subnet_id" {
  value = var.vnet == "${var.resource_prefix}-vnet" ? azurerm_subnet.init["container"].id : ""
}
