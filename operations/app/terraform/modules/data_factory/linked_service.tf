resource "azurerm_data_factory_linked_service_azure_file_storage" "sftp_share" {
  name                = "SFTPShare"
  resource_group_name = var.resource_group
  data_factory_id     = azurerm_data_factory.primary.id

  connection_string        = var.sftp_storage.primary_connection_string
  additional_properties    = {}
  annotations              = []
  file_share               = "@{linkedService().sharename}"
  integration_runtime_name = azurerm_data_factory_integration_runtime_azure.vnet.name
  parameters = {
    "sharename" = ""
  }

  timeouts {}
}

resource "azurerm_data_factory_linked_service_azure_blob_storage" "sftp_archive" {
  name                = "SFTPArchive"
  resource_group_name = var.resource_group
  data_factory_id     = azurerm_data_factory.primary.id

  connection_string        = var.sa_primary_connection_string
  additional_properties    = {}
  annotations              = []
  integration_runtime_name = azurerm_data_factory_integration_runtime_azure.vnet.name
  parameters               = {}
  use_managed_identity     = true

  timeouts {}
}
