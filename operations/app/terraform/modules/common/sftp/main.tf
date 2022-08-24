locals {
  users          = var.users
  sftp_users     = var.sshaccess
  instance_users = var.instance_users
}

resource "azurerm_storage_share" "sftp" {
  for_each = toset(local.users)

  name                 = "${var.resource_prefix}-share-${each.value}"
  storage_account_name = var.storage_account.name
  quota                = 50
}

resource "azurerm_container_group" "sftp" {
  name                = "${var.resource_prefix}-continst"
  resource_group_name = var.resource_group
  location            = var.location
  ip_address_type     = "Public" //Public until application gateways are permitted (see network.tf)
  dns_name_label      = var.resource_prefix
  #network_profile_id  = var.network_profile_id
  os_type        = "Linux"
  restart_policy = "Always"

  exposed_port = [{
    port     = 22
    protocol = "TCP"
  }]

  container {
    name   = "sftp-source"
    image  = "atmoz/sftp:latest"
    cpu    = var.cpu
    memory = var.memory
    environment_variables = {
      "SFTP_USERS" = join(" ", local.sftp_users)
    }

    ports {
      port     = 22
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
      for_each = toset(local.users)
      content {
        name       = "sftpuserauth${volume.value}"
        mount_path = "/home/${volume.value}/.ssh/keys"
        read_only  = true
        secret     = { ssh = base64encode(data.azurerm_ssh_public_key.sftp["${var.instance}-${volume.value}"].public_key) }
      }
    }

    # User file shares
    dynamic "volume" {
      for_each = toset(local.users)
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

  identity {
    type = "SystemAssigned"
  }

  depends_on = [
    azurerm_storage_share.sftp
  ]

  tags = {
    environment = var.environment
  }
}
