locals {
  default = {
    "function_app" = {
      resource_id = var.function_app_id
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
    # "front_door" = {
    #   resource_id = var.front_door_id
    # },
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
      resource_id = var.replica_vnet_id
    },
    "east_vnet" = {
      resource_id = var.primary_vnet_id
    },
    "storage_account" = {
      resource_id = var.storage_account_id
    },
    "storage_account_blob" = {
      resource_id = "${var.storage_account_id}/blobServices/default"
    },
    "storage_public" = {
      resource_id = var.storage_public_id
    },
    "storage_partner" = {
      resource_id = var.storage_partner_id
    },
    # "data_factory" = {
    #   resource_id = var.data_factory_id
    # }
    # "sftp_instance_01" = {
    #   resource_id = var.sftp_instance_01_id
    # }
  }
}

data "azurerm_monitor_diagnostic_categories" "diagnostics" {
  for_each    = local.default
  resource_id = each.value.resource_id
}
