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
