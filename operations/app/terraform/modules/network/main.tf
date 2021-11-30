/* Subnet CIDRs */
module "subnet_addresses" {
  count = length(var.vnet_names)

  source  = "hashicorp/subnets/cidr"
  version = "1.0.0"

  // VNETs can have multiple address spaces associated with them; we are using the first CIDR to create out subnets
  base_cidr_block = var.vnet_address_space[count.index][0]

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
  count = length(var.vnets)

  name                = "${var.resource_prefix}-${var.vnets[count.index].location}-nsg.public"
  location            = var.vnets[count.index].location
  resource_group_name = var.resource_group
}

resource "azurerm_network_security_group" "vnet_nsg_private" {
  count = length(var.vnets)

  name                = "${var.resource_prefix}-${var.vnets[count.index].location}-nsg.private"
  location            = var.vnets[count.index].location
  resource_group_name = var.resource_group
}


/* Public subnet */
resource "azurerm_subnet" "public_subnet" {
  count = length(var.vnets)

  name                 = "public"
  resource_group_name  = var.resource_group
  virtual_network_name = var.vnets[count.index].name
  address_prefixes = [
    module.subnet_addresses[count.index].network_cidr_blocks["public"],
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
   count = length(azurerm_subnet.public_subnet)

  subnet_id                 = azurerm_subnet.public_subnet[count.index].id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_public[count.index].id
}

/* Container subnet */
resource "azurerm_subnet" "container_subnet" {
  count = length(var.vnets)

  name                 = "container"
  resource_group_name  = var.resource_group
  virtual_network_name = var.vnets[count.index].name
  address_prefixes = [
    module.subnet_addresses[count.index].network_cidr_blocks["container"],
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
  count = length(azurerm_subnet.container_subnet)

  subnet_id                 = azurerm_subnet.container_subnet[count.index].id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_public[count.index].id
}

/* Private subnet */
resource "azurerm_subnet" "private_subnet" {
  count = length(var.vnets)

  name                 = "private"
  resource_group_name  = var.resource_group
  virtual_network_name = var.vnets[count.index].name
  address_prefixes = [
    module.subnet_addresses[count.index].network_cidr_blocks["private"],
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
  count = length(azurerm_subnet.private_subnet)

  subnet_id                 = azurerm_subnet.private_subnet[count.index].id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_private[count.index].id
}


/* Endpoint subnet */
resource "azurerm_subnet" "endpoint_subnet" {
  count = length(var.vnets)

  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = var.vnets[count.index].name
  address_prefixes = [
    module.subnet_addresses[count.index].network_cidr_blocks["endpoint"],
  ]
  service_endpoints = [
    "Microsoft.Storage",
    "Microsoft.KeyVault",
  ]

  enforce_private_link_endpoint_network_policies = true
}

resource "azurerm_subnet_network_security_group_association" "endpoint_to_nsg_private" {
  count = length(azurerm_subnet.endpoint_subnet)

  subnet_id                 = azurerm_subnet.endpoint_subnet[count.index].id
  network_security_group_id = azurerm_network_security_group.vnet_nsg_private[count.index].id
}