resource "azurerm_dashboard" "my-board" {
  name                = "test-dashboard"
  location            = var.location
  resource_group_name = var.resource_group
  tags = {
    source = "terraform"
  }
  dashboard_properties = data.template_file.dash-template.rendered
}
