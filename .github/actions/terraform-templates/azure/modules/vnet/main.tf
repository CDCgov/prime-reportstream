resource "azurerm_virtual_network" "default" {
  name                = "vnet-${var.key}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  address_space       = var.address_space.value
}

resource "azurerm_subnet" "default" {
  for_each = var.subnets

  name                 = each.key
  resource_group_name  = var.common.resource_group.name
  virtual_network_name = azurerm_virtual_network.default.name
  address_prefixes     = each.value.address_prefixes.value

  private_link_service_network_policies_enabled = each.value.link_service_policies
  private_endpoint_network_policies_enabled     = each.value.endpoint_policies

  lifecycle {
    ignore_changes = [service_endpoints]
  }
}
