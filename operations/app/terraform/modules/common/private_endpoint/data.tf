data "azurerm_subnet" "dns_subnet_id" {
  name                 = "endpoint"
  resource_group_name  = var.resource_group
  virtual_network_name = "${var.resource_prefix}-${var.dns_vnet}"
}


data "azurerm_private_endpoint_connection" "endpoint_dns" {
  name                = "${var.name}-${var.type}-${substr(sha1(data.azurerm_subnet.dns_subnet_id.id), 0, 9)}"
  resource_group_name = var.resource_group
}
