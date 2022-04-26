output "container_registry_login_server" {
  value = azurerm_container_registry.container_registry.login_server
}
output "container_registry_admin_username" {
  value = azurerm_container_registry.container_registry.admin_username
}
output "container_registry_admin_password" {
  sensitive = true
  value     = azurerm_container_registry.container_registry.admin_password
}
output "container_registry_id" {
  value = azurerm_container_registry.container_registry.id
}
