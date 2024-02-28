variable "subnet_details" {}
variable "nsg_prefix" {}
variable "resource_group" {}
variable "vnet" {}
variable "nsg_ids" {}
variable "subnet_nsg_details" {}
variable "resource_prefix" {}

resource "azurerm_subnet" "init" {
  for_each = var.subnet_details

  name                 = each.key
  resource_group_name  = var.resource_group
  address_prefixes     = ["${each.value.address_prefix}"]
  virtual_network_name = var.vnet
  service_endpoints    = each.value.service_endpoints

  dynamic "delegation" {
    for_each = toset(each.value.service_delegation)

    content {
      name = "delegation"
      service_delegation {
        name = each.value.service_delegation.0
      }
    }
  }

  lifecycle {
    ignore_changes = [
      delegation[0].service_delegation[0].actions
    ]
    prevent_destroy = true
  }
}

resource "azurerm_subnet_network_security_group_association" "init" {
  for_each = var.subnet_nsg_details

  subnet_id                 = azurerm_subnet.init[each.key].id
  network_security_group_id = var.nsg_ids["${var.nsg_prefix}${each.value.nsg}"].nsg_id

  lifecycle {
    prevent_destroy = true
  }
}
