output "law_id" {
  value = azurerm_log_analytics_workspace.law.id
}

output "service_plan_id" {
  value = data.azurerm_app_service_plan.service_plan.id
}

output "postgres_server_id" {
  value = data.azurerm_postgresql_server.postgres_server.id
}
