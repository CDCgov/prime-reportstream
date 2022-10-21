# resource "azurerm_private_dns_zone" "init" {
#   name                = "prime.local"
#   resource_group_name = var.resource_group

#   soa_record {
#     ttl          = 3600
#     minimum_ttl  = 10
#     email        = "azureprivatedns-host.microsoft.com"
#     refresh_time = 3600
#     retry_time   = 300
#     expire_time  = 2419200
#   }

# }
