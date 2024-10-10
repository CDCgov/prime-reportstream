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
    standard = {
      account_tier = "Standard"  //Standard, Premium
      account_kind = "StorageV2" //StorageV2, FileStorage, BlockBlobStorage, BlobStorage
    }
  }
  container_instances = {
    standard = {
      storage_account_key = "standard"
      os_type             = "Linux"
      image               = "ghcr.io/josiahsiegel/dev-image:latest"
      cpu_cores           = 4
      mem_gb              = 8
      commands            = []
      exec                = "/bin/bash"
      shares              = { storage = { mount_path = "/mnt/storage", gb = 500, tier = "TransactionOptimized" } } //TransactionOptimized, Premium, Hot, Cool
      repos               = { terraform-templates = { url = "https://github.com/JosiahSiegel/terraform-templates.git", mount_path = "/mnt/storage/repo1" } }
      user_password       = random_password.user_password.result
    }
  }
  dev_roles = ["Contributor", "Storage File Data Privileged Contributor", "Storage File Data SMB Share Elevated Contributor"]
}
