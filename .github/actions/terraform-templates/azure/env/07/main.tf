module "init" {
  source    = "../../modules/init/v2"
  common    = local.common
  dev_roles = local.dev_roles
}

module "azure_ad" {
  source = "../../modules/azure_ad/v2"

  common    = local.common
  active_directory_domain_name  = "${local.azure_ad.key}.local"
  active_directory_netbios_name = local.azure_ad.key
  prefix                        = local.azure_ad.key

  depends_on = [module.init]
}


module "mssql_vm" {
  for_each = local.mssql_vms

  source    = "../../modules/mssql_vm"
  common    = local.common
  key       = each.key
  ad_dns_ips = module.azure_ad.dns_ips
  domain_name = "${local.azure_ad.key}.local"
  ad_username = module.azure_ad.domain_admin_username
  ad_password = module.azure_ad.domain_admin_password

  depends_on = [module.init, module.azure_ad]
}
