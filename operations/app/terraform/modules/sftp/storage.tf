# Storage account to host file shares
resource "azurerm_storage_account" "sftp" {
  name                            = "${var.resource_prefix}sftp"
  resource_group_name             = var.resource_group
  location                        = var.location
  account_tier                    = "Standard"
  account_replication_type        = "GRS"
  min_tls_version                 = "TLS1_2"
  allow_nested_items_to_be_public = false
  large_file_share_enabled        = false

  network_rules {
    default_action = "Allow"
    bypass         = ["AzureServices"]

    ip_rules = var.terraform_caller_ip_address
    #virtual_network_subnet_ids = var.subnets.primary_subnets
  }

  tags = {
    environment = var.environment
  }

  timeouts {
    create = var.timeout_create
    read   = var.timeout_read
    delete = var.timeout_delete
    update = var.timeout_update
  }
}

# SSH host keys share
resource "azurerm_storage_share" "sftp_admin" {
  name                 = "${var.resource_prefix}-sftp-admin-share"
  storage_account_name = azurerm_storage_account.sftp.name
  quota                = 1

  timeouts {
    create = var.timeout_create
    read   = var.timeout_read
    delete = var.timeout_delete
    update = var.timeout_update
  }
}

# SFTP startup scripts share
resource "azurerm_storage_share" "sftp_scripts" {
  name                 = "${var.resource_prefix}-sftp-scripts-share"
  storage_account_name = azurerm_storage_account.sftp.name
  quota                = 1

  timeouts {
    create = var.timeout_create
    read   = var.timeout_read
    delete = var.timeout_delete
    update = var.timeout_update
  }
}

# SFTP startup script
resource "azurerm_storage_share_file" "sftp" {
  name             = "startup.sh"
  storage_share_id = azurerm_storage_share.sftp_scripts.id
  source           = "${var.sftp_dir}/startup.sh"
}
