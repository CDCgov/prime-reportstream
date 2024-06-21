##########
## Referenced by vnets.tf
##########
locals {
  vnets = {
    app-vnet = {
      name     = var.azure_vns["app-vnet"].name,
      key_name = "app-vnet"
    },
    db-vnet = {
      name     = var.azure_vns["db-vnet"].name,
      key_name = "db-vnet"
    }
  }
}

##########
## Generate subnet output variable
## START
##########
locals {
  public_subnet_ids = [for k, v in data.azurerm_subnet.public_subnet : v.id]
  #container_subnet_ids = [for k, v in data.azurerm_subnet.container_subnet : v.id]
  #private_subnet_ids   = [for k, v in data.azurerm_subnet.private_subnet : v.id]
  #endpoint_subnet_ids  = [for k, v in data.azurerm_subnet.endpoint_subnet : v.id]
  #gateway_subnet_ids   = [for k, v in data.azurerm_subnet.gateway_subnet : v.id]
  postgres_subnet_ids = [for k, v in data.azurerm_subnet.postgres_subnet : v.id]
  app_vnet_subnets = values({
    for id, details in data.azurerm_subnet.app_vnet :
    id => ({ "id" = details.id })
  }).*.id
  app_vnet    = data.azurerm_virtual_network.vnet["app-vnet"]
  db_vnet    = data.azurerm_virtual_network.vnet["db-vnet"]
}

locals {
  all_subnets             = concat(local.public_subnet_ids)
  public_endpoint_subnets = concat(local.public_subnet_ids)
  postgres_subnet         = local.postgres_subnet_ids
}

locals {
  primary_subnets                                = local.all_subnets
  #primary_endpoint_subnets                       = setsubtract(local.endpoint_subnet_ids, local.replica_subnets)
  #replica_endpoint_subnets                       = setsubtract(local.endpoint_subnet_ids, local.primary_subnets)
  primary_public_endpoint_subnets                = local.public_endpoint_subnets
  #vnet_public_container_endpoint_private_subnets = setsubtract(local.vnet_subnets, local.gateway_subnet_ids)
  #vnet_public_container_endpoint_subnets         = setsubtract(local.vnet_public_container_endpoint_private_subnets, local.private_subnet_ids)
}

locals {
  subnets = {
    all_subnets                            = local.all_subnets
    #vnet_subnets                           = local.vnet_subnets
    public_subnets                         = local.public_subnet_ids
    #replica_subnets                        = local.replica_subnets
    primary_subnets                        = local.primary_subnets
    #primary_endpoint_subnets               = local.primary_endpoint_subnets
    #replica_endpoint_subnets               = local.replica_endpoint_subnets
    public_endpoint_subnets                = local.public_endpoint_subnets
    primary_public_endpoint_subnets        = local.primary_public_endpoint_subnets
    #vnet_endpoint_subnets                  = local.vnet_endpoint_subnet
    #vnet_public_container_endpoint_subnets = local.vnet_public_container_endpoint_subnets
    postgres_subnets                       = local.postgres_subnet
  }
}
##########
## END
##########

##########
## Referenced by dns_zones.tf
##########
locals {
  dns_zones = {
    prime = {
      name = "prime.local",
      vnets = [
        local.app_vnet,
        local.db_vnet
    ] },
    azurewebsites = {
      name = "privatelink.azurewebsites.net",
      vnets = [
        local.app_vnet,
        local.db_vnet
    ] },
    blob = {
      name = "privatelink.blob.core.windows.net",
      vnets = [
        local.app_vnet
    ] },
    file = {
      name = "privatelink.file.core.windows.net",
      vnets = [
        local.app_vnet
    ] },
    postgres = {
      name = "privatelink.postgres.database.azure.com",
      vnets = [
        local.db_vnet
    ] },
    queue = {
      name = "privatelink.queue.core.windows.net",
      vnets = [
        local.app_vnet
    ] },
    servicebus = {
      name = "privatelink.servicebus.windows.net",
      vnets = [
        local.app_vnet
    ] },
    vaultcore = {
      name = "privatelink.vaultcore.azure.net",
      vnets = [
        local.app_vnet
    ] }
  }
}

locals {
  zone_vnets = distinct(flatten([
    for dns_zones in local.dns_zones : [
      for vnet in dns_zones.vnets : {
        dns_zone  = dns_zones.name
        vnet_name = vnet.name
        vnet_id   = vnet.id
      }
    ]
  ]))
}
