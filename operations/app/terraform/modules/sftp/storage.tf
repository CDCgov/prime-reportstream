# Storage account to host file shares
resource "azurerm_storage_account" "sftp" {
  name                     = "${var.resource_prefix}sftp"
  resource_group_name      = var.resource_group
  location                 = var.location
  account_tier             = "Standard"
  account_replication_type = "GRS"
  min_tls_version          = "TLS1_2"

  network_rules {
    default_action = "Allow"
    bypass         = ["AzureServices"]

    ip_rules = var.terraform_caller_ip_address
  }

  tags = {
    environment = var.environment
  }
}

# Uncomment below for initial install
# Comment and remove from state after install due to share refresh error:
# shares.Client#GetProperties: Failure sending request
# Related links:
# https://github.com/Azure/azure-rest-api-specs/issues/16782
# https://github.com/hashicorp/terraform-provider-azurerm/pull/14220
/*
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
*/