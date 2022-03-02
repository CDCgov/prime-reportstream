output "public_subnet_ids" {
  value = [for k,v in data.azurerm_subnet.public_subnet: v.id]
  }
output "container_subnet_ids" {
value = [for k,v in data.azurerm_subnet.container_subnet: v.id]
}
output "private_subnet_ids" {
value = [for k,v in data.azurerm_subnet.private_subnet: v.id]
}
output "endpoint_subnet_ids" {
value = [for k,v in data.azurerm_subnet.endpoint_subnet: v.id]
}
  