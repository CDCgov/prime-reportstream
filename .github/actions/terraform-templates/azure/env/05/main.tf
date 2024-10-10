module "init" {
  source    = "../../modules/init/v2"
  common    = local.common
  dev_roles = local.dev_roles
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
  user_password   = each.value.user_password

  depends_on = [module.init, module.storage_account]
}

output "cinst_exec" {
  value = module.container_instance[*]
}
