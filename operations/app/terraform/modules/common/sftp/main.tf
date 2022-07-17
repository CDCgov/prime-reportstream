locals {
  user_file = split("\n", file(var.users_file))
}

locals {
  file_list       = [for line in local.user_file : chomp(line)]
  file_list_names = [for line in local.user_file : element(split(":", chomp(line)), 0)]
}

resource "azurerm_storage_share" "sftp" {
  for_each = toset(local.file_list_names)

  name                 = "${var.resource_prefix}-share-${each.value}"
  storage_account_name = var.storage_account.name
  quota                = 50
}

resource "azurerm_container_group" "sftp" {
  name                = "${var.resource_prefix}-continst"
  resource_group_name = var.resource_group
  location            = var.location
  ip_address_type     = "Public"
  dns_name_label      = "${var.resource_prefix}-continst"
  os_type             = "Linux"

  container {
    name   = "sftp-source"
    image  = "josiah85/sftp:latest"
    cpu    = var.cpu
    memory = var.memory
    environment_variables = {
      "SFTP_USERS" = join(" ", local.file_list)
    }

    ports {
      port     = "22"
      protocol = "TCP"
    }

    # Admin file share
    volume {
      name                 = "sftpadmin"
      mount_path           = "/etc/sftpadmin"
      read_only            = false
      share_name           = "pdh${var.environment}-sftp-admin-share"
      storage_account_name = var.storage_account.name
      storage_account_key  = var.storage_account.primary_access_key
    }

    # Startup scripts file share
    volume {
      name                 = "sftpscripts"
      mount_path           = "/etc/sftp.d"
      read_only            = false
      share_name           = "pdh${var.environment}-sftp-scripts-share"
      storage_account_name = var.storage_account.name
      storage_account_key  = var.storage_account.primary_access_key
    }

    # User SSH public keys
    dynamic "volume" {
      for_each = toset(local.file_list_names)
      content {
        name       = "sftpuserauth${volume.value}"
        mount_path = "/home/${volume.value}/.ssh/keys"
        read_only  = true
        secret     = { ssh = base64encode(data.azurerm_ssh_public_key.sftp[volume.value].public_key) }
      }
    }

    # User file shares
    dynamic "volume" {
      for_each = toset(local.file_list_names)
      content {
        name                 = "sftpvolume${volume.value}"
        mount_path           = "/home/${volume.value}/${var.sftp_folder}"
        read_only            = false
        share_name           = "${var.resource_prefix}-share-${volume.value}"
        storage_account_name = var.storage_account.name
        storage_account_key  = var.storage_account.primary_access_key
      }
    }
  }
  tags = {
    environment = var.environment
  }
}
