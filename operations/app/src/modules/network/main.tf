/* Subnet CIDRs */
module "subnet_addresses" {
  for_each = toset(local.vnet_names)

  source  = "hashicorp/subnets/cidr"
  version = "1.0.0"

  // VNETs can have multiple address spaces associated with them; we are using the first CIDR to create out subnets
  base_cidr_block = data.azurerm_virtual_network.vnet[each.value].address_space[0]

  // If additional subnets need to be added or created in the future, read the module documentation to ensure CIDRs remain the same
  // https://registry.terraform.io/modules/hashicorp/subnets/cidr/latest
  networks = [
    {
      name     = "public"
      new_bits = 3
    },
    {
      name     = "container"
      new_bits = 3
    },
    {
      name     = "private"
      new_bits = 3
    },
    {
      name     = "endpoint"
      new_bits = 2
    },
  ]
}

/* Network security groups */
resource "azurerm_network_security_group" "vnet_nsg_public" {
  for_each = data.azurerm_virtual_network.vnet

  name                = "${var.resource_prefix}-${each.value.location}-nsg.public"
  location            = each.value.location
  resource_group_name = var.resource_group
}

resource "azurerm_network_security_group" "vnet_nsg_private" {
  for_each = data.azurerm_virtual_network.vnet

  name                = "${var.resource_prefix}-${each.value.location}-nsg.private"
  location            = each.value.location
  resource_group_name = var.resource_group
}


/* Public subnet */
resource "azurerm_subnet" "public_subnet" {
  for_each = data.azurerm_virtual_network.vnet

  name                 = "public"
  resource_group_name  = var.resource_group
  virtual_network_name = each.key
  address_prefixes = [
    module.subnet_addresses[each.key].network_cidr_blocks["public"],
  ]
  service_endpoints = [
    "Microsoft.ContainerRegistry",
    "Microsoft.Storage",
    "Microsoft.Sql",
    "Microsoft.Web",
    "Microsoft.KeyVault",
  ]
  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
      ]
    }
  }

  lifecycle {
    ignore_changes = [
      delegation[0].name, # FW team renamed this, and if we change it, a new resource will be created
    ]
  }
}

resource "azurerm_subnet_network_security_group_association" "public_to_nsg_public" {
  for_each = azurerm_subnet.public_subnet

  subnet_id                 = each.value.id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_public[each.key].id
}


/* Container subnet */
resource "azurerm_subnet" "container_subnet" {
  for_each = data.azurerm_virtual_network.vnet

  name                 = "container"
  resource_group_name  = var.resource_group
  virtual_network_name = each.key
  address_prefixes = [
    module.subnet_addresses[each.key].network_cidr_blocks["container"],
  ]
  service_endpoints = [
    "Microsoft.Storage",
    "Microsoft.KeyVault",
  ]
  delegation {
    name = "container_groups"
    service_delegation {
      name = "Microsoft.ContainerInstance/containerGroups"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
      ]
    }
  }

  lifecycle {
    ignore_changes = [
      delegation[0].name, # FW team renamed this, and if we change it, a new resource will be created
    ]
  }
}

resource "azurerm_subnet_network_security_group_association" "container_to_nsg_public" {
  for_each = azurerm_subnet.container_subnet

  subnet_id                 = each.value.id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_public[each.key].id
}


/* Private subnet */
resource "azurerm_subnet" "private_subnet" {
  for_each = data.azurerm_virtual_network.vnet

  name                 = "private"
  resource_group_name  = var.resource_group
  virtual_network_name = each.key
  address_prefixes = [
    module.subnet_addresses[each.key].network_cidr_blocks["private"],
  ]
  service_endpoints = [
    "Microsoft.Storage",
    "Microsoft.Sql",
    "Microsoft.KeyVault",
  ]

  delegation {
    name = "server_farms"
    service_delegation {
      name = "Microsoft.Web/serverFarms"
      actions = [
        "Microsoft.Network/virtualNetworks/subnets/action",
      ]
    }
  }

  lifecycle {
    ignore_changes = [
      delegation[0].name, # FW team renamed this, and if we change it, a new resource will be created
    ]
  }
}

resource "azurerm_subnet_network_security_group_association" "private_to_nsg_private" {
  for_each = azurerm_subnet.private_subnet

  subnet_id                 = each.value.id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_private[each.key].id
}


/* Endpoint subnet */
resource "azurerm_subnet" "endpoint_subnet" {
  for_each = data.azurerm_virtual_network.vnet

  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = each.key
  address_prefixes = [
    module.subnet_addresses[each.key].network_cidr_blocks["endpoint"],
  ]
  service_endpoints = [
    "Microsoft.Storage"
  ]

  enforce_private_link_endpoint_network_policies = true
}

resource "azurerm_subnet_network_security_group_association" "endpoint_to_nsg_private" {
  for_each = azurerm_subnet.endpoint_subnet

  subnet_id                 = each.value.id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_private[each.key].id
}