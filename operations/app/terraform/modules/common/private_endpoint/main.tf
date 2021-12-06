locals {
  # Create a list of common configuration options for each endpoint service
  # You would link Microsoft would have constants for these, or automatically register them, but they do not
  # Information on what constants to use is at the following URLS:
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-dns#azure-services-dns-zone-configuration
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview#private-link-resource
  options = {
    "key_vault" : {
      cnames_private    = ["privatelink.vaultcore.azure.net"]
      subresource_names = ["Vault"]
    },
    "postgres_server" : {
      cnames_private    = ["privatelink.postgres.database.azure.com"]
      subresource_names = ["postgresqlServer"]
    },
    "storage_account_blob" : {
      cnames_private    = ["privatelink.blob.core.windows.net"]
      subresource_names = ["blob"]
    },
    "storage_account_file" : {
      cnames_private    = ["privatelink.file.core.windows.net"]
      subresource_names = ["file"]
    },
    "storage_account_queue" : {
      cnames_private    = ["privatelink.queue.core.windows.net"]
      subresource_names = ["queue"]
    },
    "container_registry" : {
      cnames_private    = ["privatelink.azurecr.io"]
      subresource_names = ["registry"]
    },
    "event_hub" : {
      cnames_private    = ["privatelink.servicebus.windows.net"]
      subresource_names = ["namespace"]
    },
    "function_app" : {
      cnames_private    = ["privatelink.azurewebsites.net"]
      subresource_names = ["sites"]
    },
  }

  # Set a consistent endpoint name
  endpoint_names = { for subnet_id in var.endpoint_subnet_ids : subnet_id => "${var.name}-${var.type}-${substr(sha1(subnet_id), 0, 9)}" }

  # Make options a little easier to reference
  option = local.options[var.type]
}

resource "azurerm_private_endpoint" "endpoint" {
  count = length(var.endpoint_subnet_ids)

  name                = "${var.name}-${var.type}-${substr(sha1(var.endpoint_subnet_ids[count.index]), 0, 9)}"
  location            = var.location
  resource_group_name = var.resource_group
  subnet_id           = var.endpoint_subnet_ids[count.index]

  # Associate the endpoint with the service
  private_service_connection {
    name                           = "${var.name}-${var.type}-${substr(sha1(var.endpoint_subnet_ids[count.index]), 0, 9)}"
    private_connection_resource_id = var.resource_id
    is_manual_connection           = false
    subresource_names              = local.option.subresource_names
  }
}

# An A record is used specifically because azurerm_private_endpoint.private_dns_zone_group has an order-of-operations issue with multiple private endpoints
# resource "azurerm_private_dns_a_record" "endpoint_dns" {
#   for_each = toset(local.option.cnames_private)

#   name                = var.name
#   resource_group_name = var.resource_group
#   zone_name           = each.value

#   records = azurerm_private_endpoint.endpoint[var.endpoint_subnet_id_for_dns].custom_dns_configs[0].ip_addresses
#   ttl     = 60
# }