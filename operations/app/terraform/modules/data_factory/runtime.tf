resource "azurerm_data_factory_integration_runtime_azure" "vnet" {
  name                = "VnetIR"
  data_factory_id     = azurerm_data_factory.primary.id
  location            = var.location
  resource_group_name = var.resource_group

  cleanup_enabled         = false
  compute_type            = "General"
  core_count              = 8
  time_to_live_min        = 10
  virtual_network_enabled = true

  timeouts {}
}
