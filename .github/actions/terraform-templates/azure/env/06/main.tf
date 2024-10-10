module "init" {
  source    = "../../modules/init/v2"
  common    = local.common
  dev_roles = local.dev_roles
}

module "container_instance" {
  for_each = local.container_instances

  source    = "../../modules/container_instance"
  common    = local.common
  key       = each.key
  image     = each.value.image
  cpu_cores = each.value.cpu_cores
  mem_gb    = each.value.mem_gb
  commands  = each.value.commands
  shares    = each.value.shares
  repos     = each.value.repos
  exec      = each.value.exec
  os_type   = each.value.os_type

  depends_on = [module.init]
}

output "cinst_exec" {
  value = module.container_instance[*]
}
