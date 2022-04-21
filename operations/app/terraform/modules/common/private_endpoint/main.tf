locals {
  # Create a list of common configuration options for each endpoint service
  # You would link Microsoft would have constants for these, or automatically register them, but they do not
  # Information on what constants to use is at the following URLS:
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-dns#azure-services-dns-zone-configuration
  #   * https://docs.microsoft.com/en-us/azure/private-link/private-endpoint-overview#private-link-resource
  options = {
    "key_vault" : {
      subresource_names = ["Vault"]
    },
    "postgres_server" : {
      subresource_names = ["postgresqlServer"]
    },
    "storage_account_blob" : {
      subresource_names = ["blob"]
    },
    "storage_account_file" : {
      subresource_names = ["file"]
    },
    "storage_account_queue" : {
      subresource_names = ["queue"]
    },
    "container_registry" : {
      subresource_names = ["registry"]
    },
    "event_hub" : {
      subresource_names = ["namespace"]
    },
    "function_app" : {
      subresource_names = ["sites"]
    },
  }

  # Make options a little easier to reference
  option = local.options[var.type]
}

resource "azurerm_private_endpoint" "endpoint" {
  name                = "${var.name}-${var.type}-${substr(sha1(var.endpoint_subnet_ids), 0, 9)}"
  location            = var.location
  resource_group_name = var.resource_group
  subnet_id           = var.endpoint_subnet_ids

  # Associate the endpoint with the service
  private_service_connection {
    name                           = "${var.name}-${var.type}-${substr(sha1(var.endpoint_subnet_ids), 0, 9)}"
    private_connection_resource_id = var.resource_id
    is_manual_connection           = false
    subresource_names              = local.option.subresource_names
  }
}

# An A record is used specifically because azurerm_private_endpoint.private_dns_zone_group has an order-of-operations issue with multiple private endpoints
resource "azurerm_private_dns_a_record" "endpoint_dns" {
  # Only create record on dns vnet
  for_each = toset(regexall("${var.resource_prefix}-${var.dns_vnet}", var.endpoint_subnet_ids))

  name                = var.name
  resource_group_name = var.resource_group
  zone_name           = var.dns_zone

  records = [azurerm_private_endpoint.endpoint.private_service_connection[0].private_ip_address]
  ttl     = 60
}
