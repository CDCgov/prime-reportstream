locals {
  public_subnet_ids       = [for k, v in data.azurerm_subnet.public_subnet : v.id]
  kv_public_subnet_ids    = [for k, v in data.azurerm_subnet.public_subnet : replace(replace(v.id, "East", "east"), "West", "west")]
  container_subnet_ids    = [for k, v in data.azurerm_subnet.container_subnet : v.id]
  kv_container_subnet_ids = [for k, v in data.azurerm_subnet.container_subnet : replace(replace(v.id, "East", "east"), "West", "west")]
  private_subnet_ids      = [for k, v in data.azurerm_subnet.private_subnet : v.id]
  endpoint_subnet_ids     = [for k, v in data.azurerm_subnet.endpoint_subnet : v.id]
  kv_endpoint_subnet_ids  = [for k, v in data.azurerm_subnet.endpoint_subnet : replace(replace(v.id, "East", "east"), "West", "west")]
  gateway_subnet_ids      = [for k, v in data.azurerm_subnet.gateway_subnet : v.id]
  west_vnet_subnets = values({
    for id, details in data.azurerm_subnet.west_vnet :
    id => ({ "id" = details.id })
  }).*.id
  kv_west_vnet_subnets = values({
    for id, details in data.azurerm_subnet.west_vnet :
    id => ({ "id" = replace(details.id, "West", "west") })
  }).*.id
  peer_vnet_subnets = values({
    for id, details in data.azurerm_subnet.peer_vnet :
    id => ({ "id" = details.id })
  }).*.id
  east_vnet_subnets = values({
    for id, details in data.azurerm_subnet.east_vnet :
    id => ({ "id" = details.id })
  }).*.id
  vnet_subnets = values({
    for id, details in data.azurerm_subnet.vnet :
    id => ({ "id" = details.id })
  }).*.id
}

locals {
  all_subnets             = concat(local.public_subnet_ids, local.container_subnet_ids, local.endpoint_subnet_ids)
  public_endpoint_subnets = concat(local.public_subnet_ids, local.endpoint_subnet_ids)
  replica_subnets         = concat(local.west_vnet_subnets, local.peer_vnet_subnets)
  vnet_endpoint_subnet    = setintersection(local.vnet_subnets, local.endpoint_subnet_ids)
  vnet_public_subnet      = setintersection(local.vnet_subnets, local.public_subnet_ids)
  vnet_container_subnet   = setintersection(local.vnet_subnets, local.container_subnet_ids)
}

locals {
  primary_subnets                                = setsubtract(local.all_subnets, local.replica_subnets)
  primary_endpoint_subnets                       = setsubtract(local.endpoint_subnet_ids, local.replica_subnets)
  replica_endpoint_subnets                       = setsubtract(local.endpoint_subnet_ids, local.primary_subnets)
  primary_public_endpoint_subnets                = setsubtract(local.public_endpoint_subnets, local.replica_subnets)
  vnet_public_container_endpoint_private_subnets = setsubtract(local.vnet_subnets, local.gateway_subnet_ids)
  vnet_public_container_endpoint_subnets         = setsubtract(local.vnet_public_container_endpoint_private_subnets, local.private_subnet_ids)
}

locals {
  subnets = {
    all_subnets                            = local.all_subnets
    vnet_subnets                           = local.vnet_subnets
    public_subnets                         = local.public_subnet_ids
    replica_subnets                        = local.replica_subnets
    primary_subnets                        = local.primary_subnets
    primary_endpoint_subnets               = local.primary_endpoint_subnets
    replica_endpoint_subnets               = local.replica_endpoint_subnets
    public_endpoint_subnets                = local.public_endpoint_subnets
    primary_public_endpoint_subnets        = local.primary_public_endpoint_subnets
    vnet_endpoint_subnets                  = local.vnet_endpoint_subnet
    vnet_public_container_endpoint_subnets = local.vnet_public_container_endpoint_subnets
  }
}

locals {
  dns_zones = {
    prime = {
      name = "prime.local",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    azurewebsites = {
      name = "privatelink.azurewebsites.net",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    blob = {
      name = "privatelink.blob.core.windows.net",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    file = {
      name = "privatelink.file.core.windows.net",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    postgres = {
      name = "privatelink.postgres.database.azure.com",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    queue = {
      name = "privatelink.queue.core.windows.net",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    servicebus = {
      name = "privatelink.servicebus.windows.net",
      vnets = [
        data.azurerm_virtual_network.vnet,
        data.azurerm_virtual_network.east_vnet,
        data.azurerm_virtual_network.west_vnet
    ] },
    vaultcore = {
      name = "privatelink.vaultcore.azure.net",
      vnets = [
        data.azurerm_virtual_network.vnet
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
