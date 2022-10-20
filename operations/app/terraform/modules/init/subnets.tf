module "subnets" {
  for_each = var.network

  source             = "./subnets"
  nsg_prefix         = each.value.nsg_prefix
  resource_group     = var.resource_group
  subnet_details     = each.value.subnet_details
  vnet               = "${var.resource_prefix}-${each.key}"
  nsg_ids            = local.nsg_ids
  subnet_nsg_details = each.value.subnet_nsg_details
  resource_prefix    = var.resource_prefix

  depends_on = [
    azurerm_virtual_network.init
  ]
}
