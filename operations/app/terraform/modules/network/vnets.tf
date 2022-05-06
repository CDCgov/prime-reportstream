data "azurerm_virtual_network" "vnet" {
  for_each = local.vnets

  name                = "${var.resource_prefix}-${each.value.name}"
  resource_group_name = var.resource_group
}
