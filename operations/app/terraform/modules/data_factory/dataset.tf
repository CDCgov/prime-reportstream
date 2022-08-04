resource "azurerm_data_factory_dataset_binary" "sftp_share" {
  name                = "SFTPShare"
  resource_group_name = var.resource_group
  data_factory_id     = azurerm_data_factory.primary.id

  sftp_server_location {
    path                     = "/"
    filename                 = ""
    dynamic_filename_enabled = true
    dynamic_path_enabled     = true
  }

  linked_service_name = azurerm_data_factory_linked_service_azure_file_storage.sftp_share.name
  folder              = "SFTP-share-to-archive"
  parameters = {
    "sharename" = "testshare"
  }
  additional_properties = {}
  annotations           = []

  timeouts {}

  lifecycle {
    ignore_changes = [
      sftp_server_location #TF not recognizing file share properly
    ]
  }
}

resource "azurerm_data_factory_dataset_binary" "sftp_archive" {
  name                = "SFTPArchive"
  resource_group_name = var.resource_group
  data_factory_id     = azurerm_data_factory.primary.id

  linked_service_name = azurerm_data_factory_linked_service_azure_blob_storage.sftp_archive.name
  folder              = "SFTP-share-to-archive"
  parameters = {
    "sharename" = "testshare"
  }
  additional_properties = {}
  annotations           = []

  azure_blob_storage_location {
    container                = "sftparchive"
    dynamic_filename_enabled = false
    dynamic_path_enabled     = true
    path                     = "@concat(dataset().sharename,'/',string(formatDateTime(utcNow(),'yyyyMMddHH')))"
  }

  timeouts {}
}
