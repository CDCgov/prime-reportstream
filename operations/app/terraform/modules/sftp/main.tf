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

# SSH host keys
resource "azurerm_storage_container" "sftp" {
  name                  = "${var.resource_prefix}-sftp"
  storage_account_name  = azurerm_storage_account.sftp.name
  container_access_type = "private"
}

module "instance" {
  for_each = fileset("../../../../../.environment/sftp/instances", "*.conf")

  source          = "../common/sftp"
  resource_prefix = "${var.resource_prefix}-${replace(replace(each.value, "/[^a-z0-9]/", ""), "conf", "")}"
  resource_group  = var.resource_group
  environment     = var.environment
  cpu             = 2
  memory          = 4
  sftp_folder     = "uploads"
  location        = var.location
  users_file      = "../../../../../.environment/sftp/instances/${each.value}"
  key_vault_id    = var.key_vault_id
  storage_account = azurerm_storage_account.sftp

  depends_on = [
    azurerm_storage_container.sftp
  ]
}
