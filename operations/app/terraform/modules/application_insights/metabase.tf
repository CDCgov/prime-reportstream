resource "azurerm_application_insights" "metabase_insights" {
  count = var.is_metabase_env ? 1 : 0

  name                = "${var.resource_prefix}-metabase-appinsights"
  location            = var.location
  resource_group_name = var.resource_group
  application_type    = "web"

  # Sonarcloud flag
  internet_ingestion_enabled = false

  tags = {
    environment = var.environment
  }
}