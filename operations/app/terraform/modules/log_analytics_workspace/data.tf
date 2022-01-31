data "azurerm_application_insights" "app_insights" {
  name                = "${var.resource_prefix}-appinsights"
  resource_group_name = var.resource_group
}

data "azurerm_app_service_plan" "service_plan" {
  name                = "${var.resource_prefix}-serviceplan"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "service_plan" {
  resource_id = data.azurerm_app_service_plan.service_plan.id
}

data "azurerm_function_app" "function_app" {
  name                = "${var.resource_prefix}-functionapp"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "function_app" {
  resource_id = data.azurerm_function_app.function_app.id
}

data "azurerm_container_registry" "container_registry" {
  name                = "${var.resource_prefix}containerregistry"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "container_registry" {
  resource_id = data.azurerm_container_registry.container_registry.id
}

data "azurerm_postgresql_server" "postgres_server" {
  name                = "${var.resource_prefix}-pgsql"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "postgres_server" {
  resource_id = data.azurerm_postgresql_server.postgres_server.id
}

data "azurerm_resources" "front_door" {
  type = "Microsoft.Network/frontdoors"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "front_door" {
  resource_id = data.azurerm_resources.front_door.resources[0].id
}

data "azurerm_key_vault" "application_key_vault" {
  name                = "${var.resource_prefix}-keyvault"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "application_key_vault" {
  resource_id = data.azurerm_key_vault.application_key_vault.id
}

data "azurerm_key_vault" "app_config_key_vault" {
  name                = "${var.resource_prefix}-appconfig"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "app_config_key_vault" {
  resource_id = data.azurerm_key_vault.app_config_key_vault.id
}

data "azurerm_key_vault" "client_config_key_vault" {
  name                = "${var.resource_prefix}-clientconfig"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "client_config_key_vault" {
  resource_id = data.azurerm_key_vault.client_config_key_vault.id
}

data "azurerm_nat_gateway" "nat_gateway" {
  name                = "${var.resource_prefix}-natgateway"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "nat_gateway" {
  resource_id = data.azurerm_nat_gateway.nat_gateway.id
}

locals {
  vnets = [data.azurerm_virtual_network.east_vnet, data.azurerm_virtual_network.west_vnet]
}

data "azurerm_subnet" "public_subnet" {
  count = length(local.vnets)
  name                = "public"
  resource_group_name = var.resource_group
  virtual_network_name = local.vnets[count.index].name
}

data "azurerm_network_security_group" "vnet_nsg_private" {
  count = length(local.vnets)
  name                = "${var.resource_prefix}-${local.vnets[count.index].location}-nsg.private"
  resource_group_name = var.resource_group
}

data "azurerm_network_security_group" "vnet_nsg_public" {
  count = length(local.vnets)
  name                = "${var.resource_prefix}-${local.vnets[count.index].location}-nsg.public"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "west_vnet" {
  name                = "${var.resource_prefix}-West-vnet"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "west_vnet" {
  resource_id = data.azurerm_virtual_network.west_vnet.id
}

data "azurerm_virtual_network" "east_vnet" {
  name                = "${var.resource_prefix}-East-vnet"
  resource_group_name = var.resource_group
}
data "azurerm_monitor_diagnostic_categories" "east_vnet" {
  resource_id = data.azurerm_virtual_network.east_vnet.id
}

data "azurerm_virtual_network" "vnet" {
  name                = "${var.resource_prefix}-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "vnet_peer" {
  name                = "${var.resource_prefix}-vnet-peer"
  resource_group_name = var.resource_group
}
