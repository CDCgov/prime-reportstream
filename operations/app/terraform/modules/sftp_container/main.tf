resource "azurerm_network_profile" "sftp_network_profile" {
  name                = "sftp_network_profile"
  location            = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "sftp_container_network_interface"
    ip_configuration {
      name      = "sftp_container_ip_configuration"
      subnet_id = data.azurerm_subnet.container.id
    }
  }
}

resource "azurerm_network_profile" "sftp_vnet_network_profile" {
  name                = "sftp_vnet_network_profile"
  location            = var.location
  resource_group_name = var.resource_group

  container_network_interface {
    name = "sftp_container_vnet_network_interface"
    ip_configuration {
      name      = "sftp_container_vnet_ip_configuration"
      subnet_id = data.azurerm_subnet.container_subnet.id
    }
  }
}

resource "azurerm_container_group" "sftp_container" {
  name                = "${var.resource_prefix}-sftpserver"
  location            = var.location
  resource_group_name = var.resource_group
  ip_address_type     = "Private"
  network_profile_id  = azurerm_network_profile.sftp_vnet_network_profile.id
  os_type             = "Linux"
  restart_policy      = "Always"

  # Updated to match test environment
  exposed_port = [{
    port     = 22
    protocol = "TCP"
  }]

  container {
    name   = "${var.resource_prefix}-sftpserver"
    image  = "atmoz/sftp:alpine"
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

  lifecycle {
    # Workaround. TF thinks this is a new resource after import
    ignore_changes = [
      container[0].volume[0],
    ]
  }

  depends_on = [
    azurerm_storage_share.sftp_share,
    azurerm_network_profile.sftp_vnet_network_profile
  ]
}

resource "azurerm_storage_share" "sftp_share" {
  name                 = "${var.resource_prefix}-sftpserver"
  storage_account_name = var.storage_account.name

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
