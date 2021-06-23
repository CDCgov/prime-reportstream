locals {
  # Create a list of common configuration options for each endpoint service
  # You would link Microsoft would have constants for these, or automatically register them, but they do not
  # Information on what constants to use is at the following URLS:
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-dns#azure-services-dns-zone-configuration
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview#private-link-resource
  options = {
    "key_vault": {
      cnames_private = ["privatelink.vaultcore.azure.net"]
      subresource_names = ["Vault"]
    },
    "postgres_server": {
      cnames_private = ["privatelink.postgres.database.azure.com"]
      subresource_names = ["postgresqlServer"]
    },
    "storage_account_blob": {
      cnames_private = ["privatelink.blob.core.windows.net"]
      subresource_names = ["blob"]
    },
    "storage_account_file": {
      cnames_private = ["privatelink.file.core.windows.net"]
      subresource_names = ["file"]
    },
    "storage_account_queue": {
      cnames_private = ["privatelink.queue.core.windows.net"]
      subresource_names = ["queue"]
    },
    "container_registry": {
      cnames_private = ["privatelink.azurecr.io"]
      subresource_names = ["registry"]
    },
    "event_hub": {
      cnames_private = ["privatelink.servicebus.windows.net"]
      subresource_names = ["namespace"]
    },
    "function_app": {
      cnames_private = ["privatelink.azurewebsites.net"]
      subresource_names = ["sites"]
    },
  }

  option = local.options[var.type] # Make options a little easier to reference
  endpoint_name = "${var.name}-${var.type}"
}

resource "azurerm_private_endpoint" "endpoint" {
  name = local.endpoint_name
  location = var.location
  resource_group_name = var.resource_group
  subnet_id = var.endpoint_subnet_id

  # Associate the endpoint with the service
  private_service_connection {
    name = local.endpoint_name
    private_connection_resource_id = var.resource_id
    is_manual_connection = false
    subresource_names = local.option.subresource_names
  }

  # Automatically register the private endpoint in the private DNS zones
  dynamic "private_dns_zone_group" {
    for_each = (var.location == "eastus") ? [0] : [] # DNS not needed for peered VNET
    content {
      name = local.endpoint_name
      private_dns_zone_ids = [for dns_zone in data.azurerm_private_dns_zone.private_dns_cname : dns_zone.id]
    }
  }
}

# These are Azure's private link CNAMES
data "azurerm_private_dns_zone" "private_dns_cname" {
  for_each = toset(local.option.cnames_private)
  name = each.value
  resource_group_name = var.resource_group
}