# output "dashboard_id" {
#   description = "The resource ID of the Dashboard"
#   value       = [for k in azurerm_portal_dashboard.main : k.id]
# }

# output "dashboard_id" {
#   description = "The resource ID of the Dashboard"
#   value       = module.azure_dashboard.dashboard_id
# }