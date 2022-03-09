output "azure_vns" {
    value = azurerm_virtual_network.azure_vns
}

output "west_vnet_id" {
    value = data.azurerm_virtual_network.west_vnet.id
}

output "east_vnet_id" {
    value = data.azurerm_virtual_network.east_vnet.id
}

output "west_vnet_subnets" {
  value = values({
    for id, details in data.azurerm_subnet.west_vnet:
    id => ({"id" = details.id})
  }).*.id
}

output "east_vnet_subnets" {
  value = values({
    for id, details in data.azurerm_subnet.east_vnet:
    id => ({"id" = details.id})
  }).*.id
}

output "vnet_subnets" {
  value = values({
    for id, details in data.azurerm_subnet.vnet:
    id => ({"id" = details.id})
  }).*.id
}