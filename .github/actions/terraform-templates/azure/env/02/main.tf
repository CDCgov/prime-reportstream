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

module "azure_mssql" {
  for_each = local.mssql_instances

  source    = "../../modules/azure_mssql/v2"
  common    = local.common
  key       = each.key
  sqladmins = module.init.sqladmins
  databases = each.value.databases
  epool     = each.value.epool

  admin_username = local.key_vaults.primary.secrets.SqlUsername.value
  admin_password = local.key_vaults.primary.secrets.SqlPassword.value

  depends_on = [module.init, module.key_vault]
}

module "data_factory" {
  for_each = local.data_factories

  source = "../../modules/data_factory/v2"
  common = local.common
  roles  = each.value.roles
  key    = each.key

}