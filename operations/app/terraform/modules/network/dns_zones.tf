resource "azurerm_private_dns_zone" "zone" {
  for_each = local.dns_zones

  name                = each.value.name
  resource_group_name = var.resource_group

  lifecycle {
    ignore_changes = [
      tags
    ]
  }
}

resource "time_sleep" "wait_dns_zone" {
  create_duration = "30s"

  depends_on = [azurerm_private_dns_zone.zone]
}

resource "azurerm_private_dns_zone_virtual_network_link" "dns_zone_private_link" {
  for_each = { for entry in local.zone_vnets : "${var.resource_prefix}-${substr(entry.vnet_name, -14, -1)}-${entry.dns_zone}" => entry }

  name                  = each.value.vnet_name == data.azurerm_virtual_network.vnet["app-vnet"].name ? each.value.dns_zone : "${each.key}"
  private_dns_zone_name = each.value.dns_zone
  resource_group_name   = var.resource_group
  virtual_network_id    = each.value.vnet_id

  lifecycle {
    ignore_changes = [
      tags
    ]
  }

  depends_on = [
    time_sleep.wait_dns_zone,
    azurerm_private_dns_zone.zone
  ]
}
