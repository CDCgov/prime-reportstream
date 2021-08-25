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

  # For each VNET, create CIDRs for the VNET's subnets using the cidrsubnets function
  # https://www.terraform.io/docs/language/functions/cidrsubnets.html
  #
  # Example, given vnet1(address_space: '10.0.1.0/25') and vnet2(address_space: '10.0.2.0/25'):
  # {
  #   'vnet1': [
  #     '10.0.1.0/28',
  #     '10.0.1.16/28',
  #     '10.0.1.32/28',
  #     '10.0.1.48/28',
  #     '10.0.1.64/27',
  #   ],
  #   'vnet2': [
  #     '10.0.2.0/28',
  #     '10.0.2.16/28',
  #     '10.0.2.32/28',
  #     '10.0.2.48/28',
  #     '10.0.2.64/27',
  #   ],
  # }
  vnet_subnets_cidrs = { for vnet in data.azurerm_virtual_network.vnet : vnet.name => cidrsubnets(vnet.address_space[0], 3, 3, 3, 3, 2) }
}

data "azurerm_virtual_network" "vnet" {
  for_each            = toset(local.vnets)
  name                = each.value
  resource_group_name = var.resource_group
}