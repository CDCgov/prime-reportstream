# locals {
#   dns_zones_private = [
#     "privatelink.vaultcore.azure.net",
#     "privatelink.postgres.database.azure.com",
#     "privatelink.blob.core.windows.net",
#     "privatelink.file.core.windows.net",
#     "privatelink.queue.core.windows.net",
#     #"privatelink.azurecr.io",
#     "privatelink.servicebus.windows.net",
#     "privatelink.azurewebsites.net",
#     "prime.local",
#   ]

#   # Due to only a single DNS record allowed per resource group, some private endpoints conflicts in with multiple VNETs
#   # By omitting the DNS records, we ensure the Azure backbone is used instead of attempting to reach an unpeered VNET
#   omit_dns_zones_private_in_cdc_vnet = [
#     "privatelink.vaultcore.azure.net",
#   ]

#   vnet_primary_name = "${var.resource_prefix}-East-vnet"
#   vnet_primary      = data.azurerm_virtual_network.vnet[local.vnet_primary_name]

#   vnet_names = [
#     local.vnet_primary_name,
#     "${var.resource_prefix}-West-vnet",
#   ]
# }

# data "azurerm_virtual_network" "vnet" {
#   for_each = toset(local.vnet_names)

#   name                = each.value
#   resource_group_name = var.resource_group
# }