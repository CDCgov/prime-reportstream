resource "azurerm_data_factory" "primary" {
  name                = "${var.resource_prefix}-df"
  location            = var.location
  resource_group_name = var.resource_group

  managed_virtual_network_enabled = true
  public_network_enabled          = true

  identity {
    type = "SystemAssigned"
  }

  timeouts {}

  depends_on = [
    var.sftp_shares
  ]
}
