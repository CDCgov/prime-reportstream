output "name" {
  value = azurerm_virtual_network.default.name
}

output "meta" {
  value = azurerm_virtual_network.default
}

output "subnets" {
  value = azurerm_subnet.default
}
