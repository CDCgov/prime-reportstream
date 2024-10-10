resource "azurerm_storage_share" "default" {
  for_each = var.shares

  name                 = "cinst-share-${var.key}"
  quota                = each.value.gb
  storage_account_name = var.storage_account.name
  access_tier          = each.value.tier
}

resource "azurerm_container_group" "default" {
  name                = "cinst-${var.key}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
  ip_address_type     = "Public"
  os_type             = var.os_type
  dns_name_label      = "cinst-${var.key}"

  container {
    name     = "cinst-${var.key}"
    image    = var.image
    cpu      = var.cpu_cores
    memory   = var.mem_gb
    commands = var.commands

    secure_environment_variables = {
      USER_PASSWORD = var.user_password
    }

    ports {
      port     = 2222
      protocol = "TCP"
    }

    dynamic "volume" {
      for_each = var.shares
      content {
        name                 = volume.key
        storage_account_name = var.storage_account.name
        storage_account_key  = var.storage_account.primary_access_key
        share_name           = azurerm_storage_share.default[volume.key].name
        read_only            = false
        mount_path           = volume.value.mount_path
      }
    }

    dynamic "volume" {
      for_each = var.repos
      content {
        name       = volume.key
        mount_path = volume.value.mount_path
        git_repo {
          url       = volume.value.url
          directory = "/"
        }
      }
    }
  }
}
