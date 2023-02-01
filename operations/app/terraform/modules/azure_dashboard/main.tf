data "azurerm_subscription" "current" {}

resource "azurerm_dashboard" "az_dashboard" {
  name                = "TestPennyDashboard"
  location            = var.location
  resource_group_name = var.resource_group
  tags = {
    source      = "terraform"
    environment = var.environment
  }
  dashboard_properties = templatefile("../../../../../templates/az_dashboard_json.tpl",
    {
      subscription_id  = data.azurerm_subscription.current.subscription_id
      appinsights_name = "${var.resource_prefix}-appinsights"
      resource_group_name = var.resource_group
  })
}

resource "azurerm_dashboard" "az_perf_dashboard" {
  name                = "TestPerformDashboard"
  location            = var.location
  resource_group_name = var.resource_group
  tags = {
    source      = "terraform"
    environment = var.environment
  }
  dashboard_properties = templatefile("../../../../../templates/az_performance_dashboard_json.tpl",
    {
      subscription_id  = data.azurerm_subscription.current.subscription_id
      appinsights_name = "${var.resource_prefix}-appinsights"
      resource_group_name = var.resource_group
  })
}

