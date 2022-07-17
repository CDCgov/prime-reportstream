locals {
  sftp_dir = "../../../../../.environment/sftp"
  sshnames = jsondecode(data.external.sftp_ssh_query.result.sshnames)
}

# Fetch names of related SSH keys
# SSH key names determine SFTP instances and users
data "external" "sftp_ssh_query" {
  program = ["bash", "${local.sftp_dir}/get_ssh_list.sh"]

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
  source           = "${local.sftp_dir}/startup.sh"
}

module "instance" {
  for_each = toset(jsondecode(data.external.sftp_ssh_query.result.instances))

  source          = "../common/sftp"
  instance        = each.value
  resource_prefix = "${var.resource_prefix}-${replace(replace(each.value, "/[^a-z0-9]/", ""), "conf", "")}"
  resource_group  = var.resource_group
  environment     = var.environment
  cpu             = 2
  memory          = 4
  sftp_folder     = "uploads"
  location        = var.location
  users           = [for item in local.sshnames : replace(item, "${var.resource_prefix}-${each.value}-", "") if can(regex("${each.value}-", item))]
  instance_users  = [for item in local.sshnames : replace(item, "${var.resource_prefix}-", "") if can(regex("${each.value}-", item))]
  sshaccess       = [for item in local.sshnames : "${replace(item, "${var.resource_prefix}-${each.value}-", "")}:::100" if can(regex("${each.value}-", item))]
  key_vault_id    = var.key_vault_id
  storage_account = azurerm_storage_account.sftp
  admin_share     = azurerm_storage_share.sftp_admin
  scripts_share   = azurerm_storage_share.sftp_scripts

  depends_on = [
    azurerm_storage_share_file.sftp
  ]
}
