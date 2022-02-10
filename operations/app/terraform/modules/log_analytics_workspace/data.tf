locals {
  default = {
    "function_app" = {
      resource_id = data.azurerm_function_app.function_app.id
    },
    "service_plan" = {
      resource_id = var.service_plan_id
    },
    "container_registry" = {
      resource_id = var.container_registry_id
    },
    "postgres_server" = {
      resource_id = var.postgres_server_id
    },
    "front_door" = {
      resource_id = data.azurerm_resources.front_door.resources[0].id
    },
    "application_key_vault" = {
      resource_id = var.application_key_vault_id
    },
    "app_config_key_vault" = {
      resource_id = var.app_config_key_vault_id
    },
    "client_config_key_vault" = {
      resource_id = var.client_config_key_vault_id
    },
    "west_vnet" = {
      resource_id = data.azurerm_virtual_network.west_vnet.id
    },
    "east_vnet" = {
      resource_id = data.azurerm_virtual_network.east_vnet.id
    },
    "storage_account" = {
      resource_id = data.azurerm_storage_account.storage_account.id
    },
    "storage_public" = {
      resource_id = data.azurerm_storage_account.storage_public.id
    },
    "storage_partner" = {
      resource_id = data.azurerm_storage_account.storage_partner.id
    }
  }
}

data "azurerm_monitor_diagnostic_categories" "diagnostics" {
  for_each                   = local.default
  resource_id = each.value.resource_id
}

data "azurerm_function_app" "function_app" {
  name                = "${var.resource_prefix}-functionapp"
  resource_group_name = var.resource_group
}

data "azurerm_resources" "front_door" {
  type = "Microsoft.Network/frontdoors"
  resource_group_name = var.resource_group
}

data "azurerm_nat_gateway" "nat_gateway" {
  name                = "${var.resource_prefix}-natgateway"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "west_vnet" {
  name                = "${var.resource_prefix}-West-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "east_vnet" {
  name                = "${var.resource_prefix}-East-vnet"
  resource_group_name = var.resource_group
}

data "azurerm_storage_account" "storage_account" {
  name                = "${var.resource_prefix}storageaccount"
  resource_group_name = var.resource_group
}

data "azurerm_storage_account" "storage_public" {
  name                = "${var.resource_prefix}public"
  resource_group_name = var.resource_group
}

data "azurerm_storage_account" "storage_partner" {
  name                = "${var.resource_prefix}partner"
  resource_group_name = var.resource_group
}
