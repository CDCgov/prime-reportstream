locals {
  sftp_dir = "../../../../../.environment/sftp"
}

# terraform -chdir=operations/app/terraform/vars/test state show module.sftp.data.external.sftp_ssh_query
data "external" "sftp_ssh_query" {
  program = ["bash", "${local.sftp_dir}/query/get_ssh_list.sh"]

  query = {
    environment = "${var.environment}"
  }
}

# Storage account to host file shares
resource "azurerm_storage_account" "sftp" {
  name                     = "${var.resource_prefix}sftp"
  resource_group_name      = var.resource_group
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "LRS"

  tags = {
    environment = var.environment
  }
}

# SSH host keys share
resource "azurerm_storage_share" "sftp_admin" {
  name                 = "${var.resource_prefix}-sftp-admin-share"
  storage_account_name = azurerm_storage_account.sftp.name
  quota                = 1
}

# SFTP startup scripts share
resource "azurerm_storage_share" "sftp_scripts" {
  name                 = "${var.resource_prefix}-sftp-scripts-share"
  storage_account_name = azurerm_storage_account.sftp.name
  quota                = 1
}

# SFTP startup script
resource "azurerm_storage_share_file" "sftp" {
  name             = "startup.sh"
  storage_share_id = azurerm_storage_share.sftp_scripts.id
  source           = "${local.sftp_dir}/admin/startup.sh"
}

module "instance" {
  for_each = fileset("${local.sftp_dir}/instances", "*.conf")

  source          = "../common/sftp"
  resource_prefix = "${var.resource_prefix}-${replace(replace(each.value, "/[^a-z0-9]/", ""), "conf", "")}"
  resource_group  = var.resource_group
  environment     = var.environment
  cpu             = 2
  memory          = 4
  sftp_folder     = "uploads"
  location        = var.location
  users_file      = "${local.sftp_dir}/instances/${each.value}"
  key_vault_id    = var.key_vault_id
  storage_account = azurerm_storage_account.sftp
  admin_share     = azurerm_storage_share.sftp_admin
  scripts_share   = azurerm_storage_share.sftp_scripts

  depends_on = [
    azurerm_storage_share_file.sftp
  ]
}
