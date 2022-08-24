resource "azurerm_network_profile" "sftp" {
  name                = "sftp"
  location            = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "sftp"
    ip_configuration {
      name      = "sftp"
      subnet_id = data.azurerm_subnet.container_subnet.id
    }
  }
}
