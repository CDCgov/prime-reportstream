/* Public subnet */
data "azurerm_subnet" "public_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "public") }
  name                 = "public"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.azure_vns[each.key].name}"
}

/* Container subnet */
data "azurerm_subnet" "container_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "container") }
  name                 = "container"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.azure_vns[each.key].name}"
}

/* Private subnet */
data "azurerm_subnet" "private_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "private") }
  name                 = "private"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.azure_vns[each.key].name}"
}

/* Endpoint subnet */
data "azurerm_subnet" "endpoint_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "endpoint") }
  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.azure_vns[each.key].name}"
}

/* Gateway subnet */
data "azurerm_subnet" "gateway_subnet" {
  for_each             = { for k, v in var.azure_vns : k => v if contains(var.azure_vns[k].subnets, "GatewaySubnet") }
  name                 = "GatewaySubnet"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.azure_vns[each.key].name}"
}

/* Postgres subnet */
data "azurerm_subnet" "postgres_subnet" {
  for_each = toset(data.azurerm_virtual_network.vnet["db-vnet"].subnets)
  name                 = each.value
  resource_group_name  = var.resource_group
  virtual_network_name = var.azure_vns["db-vnet"].name
}

data "azurerm_subnet" "west_vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet["west"].subnets)

  name                 = each.value
  virtual_network_name = "${var.azure_vns["West-vnet"].name}"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "app_vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet["app-vnet"].subnets)

  name                 = each.value
  virtual_network_name = "${var.azure_vns["app-vnet"].name}"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet["default"].subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-vnet"
  resource_group_name  = var.resource_group
}

data "azurerm_subnet" "peer_vnet" {
  for_each = toset(data.azurerm_virtual_network.vnet["peer"].subnets)

  name                 = each.value
  virtual_network_name = "${var.resource_prefix}-vnet-peer"
  resource_group_name  = var.resource_group
}
