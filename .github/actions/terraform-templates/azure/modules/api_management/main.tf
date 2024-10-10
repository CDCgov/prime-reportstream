resource "azurerm_api_management" "default" {
  name                = "apim-${var.common.uid}-${var.common.env}"
  resource_group_name = var.common.resource_group.name
  location            = var.common.location
  publisher_name      = var.common.env
  publisher_email     = var.common.owner_email

  sku_name = "Consumption_0"

  identity {
    type = "SystemAssigned"
  }
}
