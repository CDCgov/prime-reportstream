output "instrumentation_key" {
  value = azurerm_application_insights.app_insights.instrumentation_key
}

output "app_id" {
  value = azurerm_application_insights.app_insights.app_id
}

output "connection_string" {
  value = azurerm_application_insights.app_insights.connection_string
}

output "metabase_instrumentation_key" {
  value = var.is_metabase_env ? azurerm_application_insights.metabase_insights[0].instrumentation_key : null
}

output "metabase_app_id" {
  value = var.is_metabase_env ? azurerm_application_insights.metabase_insights[0].app_id : null
}

output "metabase_connection_string" {
  value = var.is_metabase_env ? azurerm_application_insights.metabase_insights[0].connection_string : null
}

output "action_group_businesshours_id" {
  value = local.action_group_businesshours_id
}
