output "instrumentation_key" {
  value = azurerm_application_insights.app_insights.instrumentation_key
}

output "app_id" {
  value = azurerm_application_insights.app_insights.app_id
}

output "metabase_instrumentation_key" {
  value = var.is_metabase_env ? azurerm_application_insights.metabase_insights[0].instrumentation_key : null
}

output "metabase_app_id" {
  value = var.is_metabase_env ? azurerm_application_insights.metabase_insights[0].app_id : null
}