# Storage account to host file shares
resource "azurerm_storage_account" "sftp" {
  name                     = "${var.resource_prefix}sftp"
  resource_group_name      = var.resource_group
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  min_tls_version          = "TLS1_2"

  tags = {
    environment = var.environment
  }
}

resource "azurerm_storage_account_network_rules" "sftp" {
  storage_account_id = azurerm_storage_account.sftp.id

  default_action = "Deny"
  ip_rules       = [for k, v in module.instance : v.ip_address]
  bypass         = ["AzureServices"]

  depends_on = [
    module.instance
  ]
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
  source           = "${var.sftp_dir}/startup.sh"
}
