resource "azurerm_data_factory_managed_private_endpoint" "sftp_share" {
  name               = "SFTPShare"
  data_factory_id    = azurerm_data_factory.primary.id
  target_resource_id = var.sftp_storage.id
  subresource_name   = "file"

  lifecycle {
    ignore_changes = [
      fqdns
    ]
  }

  timeouts {}
}

resource "azurerm_data_factory_managed_private_endpoint" "sftp_archive" {
  name               = "StorageAccountPrivate"
  data_factory_id    = azurerm_data_factory.primary.id
  target_resource_id = var.storage_account_id
  subresource_name   = "blob"

  lifecycle {
    ignore_changes = [
      fqdns
    ]
  }

  timeouts {}
}
