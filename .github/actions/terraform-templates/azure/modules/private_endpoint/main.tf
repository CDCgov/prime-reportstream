resource "azurerm_private_endpoint" "default" {
  name                = "pe-${var.key}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  subnet_id           = var.subnet_id

  private_dns_zone_group {
    name                 = "pe-${var.key}-dns-group"
    private_dns_zone_ids = var.dns_zone_ids
  }

  private_service_connection {
    name                           = "pe-${var.key}-sc"
    private_connection_resource_id = var.resource_id
    subresource_names              = var.subresource_names
    is_manual_connection           = var.is_manual_connection
  }
}
