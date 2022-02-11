output "azure_vns" {
    value = azurerm_virtual_network.azure_vns
}

output "west_vnet_id" {
    value = data.azurerm_virtual_network.west_vnet.id
}

output "east_vnet_id" {
    value = data.azurerm_virtual_network.east_vnet.id
}
