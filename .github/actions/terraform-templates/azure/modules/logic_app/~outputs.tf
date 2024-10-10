output "ids" {
  value = azurerm_logic_app_workflow.default.identity[0]
}

output "name" {
  value = azurerm_logic_app_workflow.default.name
}

output "meta" {
  value = azurerm_logic_app_workflow.default
}

output "endpoint" {
  value     = azurerm_logic_app_trigger_http_request.default.callback_url
  sensitive = true
}
