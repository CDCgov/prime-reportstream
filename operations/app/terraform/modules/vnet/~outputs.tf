output "ids" {
  value = [azurerm_virtual_network.east.id] #[azurerm_virtual_network.west.id, azurerm_virtual_network.east.id]
}

output "names" {
    value = [azurerm_virtual_network.east.name] #[azurerm_virtual_network.west.name, azurerm_virtual_network.east.name]
}

output "vnet_address_spaces" {
  description = "The address space of the newly created vNet"
  value       = [azurerm_virtual_network.east.address_space] #[azurerm_virtual_network.west.address_space, azurerm_virtual_network.east.address_space]
}

output "vnets" {
  value = [azurerm_virtual_network.east] #[azurerm_virtual_network.west, azurerm_virtual_network.east]
}