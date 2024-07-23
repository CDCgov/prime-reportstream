resource "azurerm_container_group" "sftp_container" {
  name                = "${var.resource_prefix}-sftpserver"
  location            = var.location
  resource_group_name = var.resource_group
  ip_address_type     = "Private"
  subnet_ids          = [data.azurerm_subnet.container_subnet.id]
  os_type             = "Linux"
  restart_policy      = "Always"

  # Updated to match test environment
  exposed_port = [{
    port     = 22
    protocol = "TCP"
  }]

  container {
    name   = "${var.resource_prefix}-sftpserver"
    image  = "ghcr.io/cdcgov/prime-reportstream_sftp:alpine"
    cpu    = 1.0
    memory = 1.5

    ports {
      port     = 22
      protocol = "TCP"
    }

    environment_variables = {
      "SFTP_USERS" = "foo:pass:::upload"
    }

    volume {
      name                 = "${var.resource_prefix}-sftpserver"
      share_name           = azurerm_storage_share.sftp_share.name
      mount_path           = "/home/foo/upload"
      storage_account_name = var.storage_account.name
      storage_account_key  = var.sa_primary_access_key
    }
  }

  identity {
    type = "SystemAssigned"
  }

  tags = {
    environment = var.environment
  }

  depends_on = [
    azurerm_storage_share.sftp_share
  ]
}

resource "azurerm_storage_share" "sftp_share" {
  name                 = "${var.resource_prefix}-sftpserver"
  storage_account_name = var.storage_account.name
  quota                = 5120
  depends_on = [
    var.storage_account
  ]
}

resource "azurerm_private_dns_a_record" "sftp_prime_local" {
  name = "sftp"

  resource_group_name = var.resource_group
  zone_name           = var.dns_zones["prime"].name

  records = [
    azurerm_container_group.sftp_container.ip_address,
  ]
  ttl = 60
}
