locals {
  nsg_rules = {
    AllowVnetInBound = {
      priority            = 65000,
      direction           = "Inbound",
      access              = "Allow",
      source_address      = "VirtualNetwork",
      destination_address = "VirtualNetwork"
    },
    AllowAzureLoadBalancerInBound = {
      priority            = 65001,
      direction           = "Inbound",
      access              = "Allow",
      source_address      = "AzureLoadBalancer",
      destination_address = "*"
    },
    DenyAllInBound = {
      priority            = 65500,
      direction           = "Inbound",
      access              = "Deny",
      source_address      = "*",
      destination_address = "*"
    },
    AllowVnetOutBound = {
      priority            = 65000,
      direction           = "Outbound",
      access              = "Allow",
      source_address      = "VirtualNetwork",
      destination_address = "VirtualNetwork"
    },
    AllowInternetOutBound = {
      priority            = 65001,
      direction           = "Outbound",
      access              = "Allow",
      source_address      = "*",
      destination_address = "Internet"
    },
    DenyAllOutBound = {
      priority            = 65500,
      direction           = "Outbound",
      access              = "Allow",
      source_address      = "*",
      destination_address = "*"
    }
  }
}

locals {
  nsg_ids = {
    eastus-private = {
      nsg_id = azurerm_network_security_group.private["East-vnet"].id
    },
    eastus-public = {
      nsg_id = azurerm_network_security_group.public["East-vnet"].id
    },
    westus-private = {
      nsg_id = azurerm_network_security_group.private["West-vnet"].id
    },
    westus-public = {
      nsg_id = azurerm_network_security_group.public["West-vnet"].id
    },
    private = {
      nsg_id = azurerm_network_security_group.private["vnet"].id
    },
    public = {
      nsg_id = azurerm_network_security_group.public["vnet"].id
    }
  }
}

resource "azurerm_network_security_group" "public" {
  for_each = { for k, v in var.network : k => v if contains(var.network[k].network_security_groups, "public") }

  name                = "${var.resource_prefix}-${var.network[each.key].nsg_prefix}nsg.public"
  location            = var.network[each.key].location
  resource_group_name = var.resource_group
}

resource "azurerm_network_security_group" "private" {
  for_each = { for k, v in var.network : k => v if contains(var.network[k].network_security_groups, "private") }

  name                = "${var.resource_prefix}-${var.network[each.key].nsg_prefix}nsg.private"
  location            = var.network[each.key].location
  resource_group_name = var.resource_group
}
