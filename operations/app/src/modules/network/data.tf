locals {
  dns_zones_private = [
    "privatelink.vaultcore.azure.net",
    "privatelink.postgres.database.azure.com",
    "privatelink.blob.core.windows.net",
    "privatelink.file.core.windows.net",
    "privatelink.queue.core.windows.net",
    #"privatelink.azurecr.io",
    "privatelink.servicebus.windows.net",
    "privatelink.azurewebsites.net"
  ]

  vnets = [
    "${var.resource_prefix}-East-vnet",
    "${var.resource_prefix}-West-vnet",
  ]
  vnet_primary = data.azurerm_virtual_network.vnet[local.vnets[0]]

  vnet_subnets = { for vnet in data.azurerm_virtual_network.vnet : vnet.name => cidrsubnets(vnet.address_space[0], 3, 3, 3, 3, 2) }
}

data "azurerm_virtual_network" "vnet" {
  for_each            = toset(local.vnets)
  name                = each.value
  resource_group_name = var.resource_group
}