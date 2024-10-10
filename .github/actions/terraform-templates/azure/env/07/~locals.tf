variable "environment" {}
variable "owner_object_id" {}
variable "owner_email" {}
variable "uid" {}
variable "location" {}
variable "resource_group" {}

locals {
  env = var.environment
  common = {
    env            = local.env
    uid            = var.uid
    subscription   = data.azurerm_subscription.default
    tenant_id      = data.azurerm_subscription.default.tenant_id
    location       = var.location
    resource_group = data.azurerm_resource_group.default
    owner_email    = var.owner_email
    owner          = data.azuread_users.owner.users[0]
  }
  storage_accounts = {
  }
  mssql_vms = {
    dev = {}
  }
  azure_ad = {
    key = var.uid
    domain_name = "josiah0601gmail.onmicrosoft.com"
  }
  container_instances = {
    windev = {
      os_type   = "Windows"
      image     = "mcr.microsoft.com/windows:1809"
      cpu_cores = 2
      mem_gb    = 4
      commands  = ["cmd.exe", "/c", "ping -t localhost > NUL"]
      exec      = ""
      shares    = {}
      repos     = {}
    }
  }
  dev_roles = []
}
