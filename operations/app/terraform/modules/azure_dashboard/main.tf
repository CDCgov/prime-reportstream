data "azurerm_subscription" "current" {}

resource "azurerm_dashboard" "az_dashboard" {
  name                = "${var.resource_prefix}-RS-Dashboard"
  location            = var.location
  resource_group_name = var.resource_group
  tags = {
    source      = "terraform"
    environment = var.environment
  }
  dashboard_properties = templatefile("../../../../../templates/az_dashboard_json.tpl",
    {
      subscription_id     = data.azurerm_subscription.current.subscription_id
      appinsights_name    = "${var.resource_prefix}-appinsights"
      resource_group_name = var.resource_group
  })
}

