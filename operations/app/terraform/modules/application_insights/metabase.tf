resource "azurerm_application_insights" "metabase_insights" {
  count = var.is_metabase_env ? 1 : 0

  name                = "${var.resource_prefix}-metabase-appinsights"
  location            = var.location
  resource_group_name = var.resource_group
  application_type    = "web"

  # Sonarcloud flag
  # needs to be true so the front-end can send events to App Insights
  # https://github.com/CDCgov/prime-reportstream/issues/3097
  internet_ingestion_enabled = true
  # needs to be true to allow engineer debugging through Azure UI
  internet_query_enabled = true

  tags = {
    environment = var.environment
  }
}
