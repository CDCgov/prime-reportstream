resource "azurerm_virtual_network" "init" {
  for_each = var.network

  name                = "${var.resource_prefix}-${each.key}"
  location            = each.value.location
  resource_group_name = var.resource_group
  address_space       = [each.value.address_space]

  lifecycle {
    prevent_destroy = true
  }
}

resource "azurerm_virtual_network_peering" "init_east" {
  name                      = "${var.resource_prefix}-peer-East-to-West"
  resource_group_name       = var.resource_group
  virtual_network_name      = azurerm_virtual_network.init["East-vnet"].name
  remote_virtual_network_id = azurerm_virtual_network.init["West-vnet"].id

  depends_on = [
    azurerm_virtual_network.init
  ]
}

resource "azurerm_virtual_network_peering" "init_west" {
  name                      = "${var.resource_prefix}-peer-West-to-East"
  resource_group_name       = var.resource_group
  virtual_network_name      = azurerm_virtual_network.init["West-vnet"].name
  remote_virtual_network_id = azurerm_virtual_network.init["East-vnet"].id

  depends_on = [
    azurerm_virtual_network.init
  ]
}
