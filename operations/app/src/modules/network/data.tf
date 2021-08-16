locals {
    dns_zones_private = [
        "privatelink.vaultcore.azure.net",
        "privatelink.postgres.database.azure.com",
        "privatelink.blob.core.windows.net",
        "privatelink.file.core.windows.net",
        "privatelink.queue.core.windows.net",
        #"privatelink.azurecr.io",
        "privatelink.servicebus.windows.net",
        "privatelink.azurewebsites.net"
    ]
}

data "azurerm_virtual_network" "primary" {
    name                = "${var.resource_prefix}-East-vnet"
    resource_group_name = var.resource_group
}

data "azurerm_virtual_network" "secondary" {
    name                = "${var.resource_prefix}-West-vnet"
    resource_group_name = var.resource_group
}