/* Subnet CIDRs */
module "subnet_addresses" {
  for_each = var.azure_vns

  source  = "hashicorp/subnets/cidr"
  version = "1.0.0"

  // VNETs can have multiple address spaces associated with them; we are using the first CIDR to create out subnets
  base_cidr_block = each.value.address_space

  // If additional subnets need to be added or created in the future, read the module documentation to ensure CIDRs remain the same
  // https://registry.terraform.io/modules/hashicorp/subnets/cidr/latest
  networks = each.value.subnet_cidrs
}
/* Public subnet */
resource "azurerm_subnet" "public_subnet" {
  for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "public")}

  name                 = "public"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
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

/* Container subnet */
resource "azurerm_subnet" "container_subnet" {
  for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "container")}

  name                 = "container"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
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

/* Private subnet */
resource "azurerm_subnet" "private_subnet" {
  for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "private")}

  name                 = "private"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
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

/* Endpoint subnet */
resource "azurerm_subnet" "endpoint_subnet" {
  for_each = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "endpoint")}

  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${each.key}"
  address_prefixes = [
    module.subnet_addresses[each.key].network_cidr_blocks["endpoint"],
  ]
  service_endpoints = [
    "Microsoft.Storage",
    "Microsoft.KeyVault",
  ]

  enforce_private_link_endpoint_network_policies = true
}
