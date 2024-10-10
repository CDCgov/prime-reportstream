module "init" {
  source    = "../../modules/init/v1"
  common    = local.common
  dev_roles = local.dev_roles
}

module "key_vault" {
  for_each = local.key_vaults

  source  = "../../modules/key_vault"
  common  = local.common
  secrets = each.value.secrets
  key     = each.key

  depends_on = [module.init]
}

module "cosmosdb_postgresql" {
  for_each = local.cosmosdb_postgresql

  source         = "../../modules/cosmosdb_postgresql/v1"
  common         = local.common
  key            = each.key
  admin_password = local.key_vaults.primary.secrets.postgresPass.value

  depends_on = [module.init]
}

module "vnet" {
  for_each = local.vnets

  source        = "../../modules/vnet"
  common        = local.common
  address_space = each.value.address_space
  subnets       = each.value.subnets
  key           = each.key

  depends_on = [module.init]
}

module "dns_zone" {
  for_each = local.dns_zones

  source    = "../../modules/dns_zone"
  common    = local.common
  dns_name  = each.value.name
  dns_links = each.value.vnet_links
  key       = each.key

  vnets = { for vnet in module.vnet : vnet.meta.name => vnet.meta.id }

  depends_on = [module.init, module.vnet]
}

module "private_endpoint" {
  for_each = local.private_endpoints

  source               = "../../modules/private_endpoint"
  common               = local.common
  key                  = each.key
  subresource_names    = each.value.subresource_names
  is_manual_connection = each.value.is_manual_connection

  subnet_id    = module.vnet[each.value.vnet_key].subnets[each.value.subnet_key].id
  dns_zone_ids = [lookup({ for zone in module.dns_zone : zone.meta.name => zone.meta.id }, each.value.dns_zone_key, "")]
  resource_id  = lookup({ for psql in module.cosmosdb_postgresql : psql.meta.name => psql.meta.id }, "cluster-${local.common.uid}-${local.common.env}-${each.value.resource_id_key}", "")

  depends_on = [module.init, module.vnet, module.dns_zone, module.cosmosdb_postgresql]
}

module "storage_account" {
  for_each = local.storage_accounts

  source       = "../../modules/storage_account/v2"
  common       = local.common
  key          = each.key
  account_tier = each.value.account_tier
  account_kind = each.value.account_kind

  depends_on = [module.init]
}

module "container_instance" {
  for_each = local.container_instances

  source          = "../../modules/container_instance"
  common          = local.common
  key             = each.key
  storage_account = module.storage_account[each.value.storage_account_key].meta
  image           = each.value.image
  cpu_cores       = each.value.cpu_cores
  mem_gb          = each.value.mem_gb
  commands        = each.value.commands
  shares          = each.value.shares
  repos           = each.value.repos
  exec            = each.value.exec
  os_type         = each.value.os_type

  depends_on = [module.init, module.storage_account, module.vnet, module.key_vault]
}

output "cinst_exec" {
  value = module.container_instance[*]
}
