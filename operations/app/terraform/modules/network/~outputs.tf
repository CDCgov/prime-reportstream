output "public_subnet_ids" {
  value = azurerm_subnet.public_subnet[*].id
  }
  output "container_subnet_ids" {
  value = azurerm_subnet.container_subnet[*].id
  }
  output "private_subnet_ids" {
  value = azurerm_subnet.private_subnet[*].id
  }
  output "endpoint_subnet_ids" {
  value = azurerm_subnet.endpoint_subnet[*].id
  }
  