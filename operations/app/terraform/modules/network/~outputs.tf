output "public_subnet_ids" {
  value = local.public_subnet_ids
}
output "container_subnet_ids" {
  value = local.container_subnet_ids
}
output "private_subnet_ids" {
  value = local.private_subnet_ids
}
output "endpoint_subnet_ids" {
  value = local.endpoint_subnet_ids
}

# Key vault friendly case
output "kv_public_subnet_ids" {
  value = local.kv_public_subnet_ids
}
output "kv_container_subnet_ids" {
  value = local.kv_container_subnet_ids
}
output "kv_endpoint_subnet_ids" {
  value = local.kv_endpoint_subnet_ids
}

# Vnet outputs

output "west_vnet_id" {
  value = data.azurerm_virtual_network.west_vnet.id
}

output "east_vnet_id" {
  value = data.azurerm_virtual_network.east_vnet.id
}

output "subnets" {
  value = local.subnets
}

output "dns_zones" {
  value = azurerm_private_dns_zone.zone
}

output "zone_vnets" {
  value = local.zone_vnets
}

# output "west_vnet_subnets" {
#   value = local.west_vnet_subnets
# }

# Key vault friendly case
# output "kv_west_vnet_subnets" {
#   value = local.kv_west_vnet_subnets
# }

# output "east_vnet_subnets" {
#   value = local.east_vnet_subnets
# }

# output "vnet_subnets" {
#   value = local.vnet_subnets
# }

# output "peer_vnet_subnets" {
#   value = local.peer_vnet_subnets
# }

# output "primary_subnets" {
#   value = local.primary_subnets
# }

# output "primary_endpoint_subnets" {
#   value = local.primary_endpoint_subnets
# }

# output "replica_subnets" {
#   value = local.replica_subnets
# }

# output "replica_endpoint_subnets" {
#   value = local.replica_endpoint_subnets
# }

# output "public_endpoint_subnets" {
#   value = local.public_endpoint_subnets
# }

# output "primary_public_endpoint_subnets" {
#   value = local.primary_public_endpoint_subnets
# }
