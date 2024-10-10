module "init" {
  source    = "../../modules/init/v1"
  common    = local.common
  dev_roles = local.dev_roles
}

module "logic_app" {
  source          = "../../modules/logic_app"
  common          = local.common
  storage_account = module.storage_account.meta
  workflow_file   = "workflow.json"
  sql_server_fqdn = module.azure_db.fqdn
  db_name         = module.azure_db.db_name

  depends_on = [module.init]
}

module "azure_db" {
  source         = "../../modules/azure_mssql/v1"
  common         = local.common
  logic_app_ids  = module.logic_app.ids
  logic_app_name = module.logic_app.name

  depends_on = [module.init]
}

module "storage_account" {
  source    = "../../modules/storage_account/v1"
  common    = local.common
  logic_app = module.logic_app.ids

  depends_on = [module.init]
}

module "api_management" {
  source             = "../../modules/api_management"
  common             = local.common
  logic_app_endpoint = module.logic_app.endpoint

  depends_on = [module.init]
}

module "data_factory" {
  source          = "../../modules/data_factory/v1"
  common          = local.common
  storage_account = module.storage_account.meta
  key_vault       = module.key_vault["primary"].meta
  secrets         = module.key_vault["primary"].secrets

  depends_on = [module.init]
}

module "key_vault" {
  for_each = local.key_vaults

  source  = "../../modules/key_vault"
  common  = local.common
  secrets = each.value.secrets
  key     = each.key

  depends_on = [module.init]
}
