resource "azurerm_data_factory_trigger_schedule" "sftp_share_to_archive" {
  name                = "NightlySFTPtoArchive"
  resource_group_name = var.resource_group
  data_factory_id     = azurerm_data_factory.primary.id
  pipeline_name       = azurerm_data_factory_pipeline.sftp_share_to_archive.name

  interval    = 1
  frequency   = "Day"
  start_time  = "2022-07-18T15:29:00Z"
  activated   = true
  annotations = []

  schedule {
    days_of_month = []
    days_of_week  = []
    hours = [
      4,
    ]
    minutes = [
      40,
    ]
  }

  timeouts {}
}
