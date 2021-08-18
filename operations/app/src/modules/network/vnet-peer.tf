resource "azurerm_virtual_network_peering" "vnet_peer" {
    for_each = setsubtract([local.vnet_primary.name], toset(local.vnets))

    name                      = "${var.resource_prefix}-peer-${each.value.name}"
    resource_group_name       = var.resource_group
    virtual_network_name      = local.vnet_primary.name
    remote_virtual_network_id = each.value.id
}