data "azurerm_virtual_network" "vnet" {
  for_each = local.vnets

  name                = "${each.value.name}"
  resource_group_name = var.resource_group
}
