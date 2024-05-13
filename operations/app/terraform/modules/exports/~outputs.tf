output "resource_group_30day_cost_export_id" {
    description = "The ID of the Cost Management Export for this Resource Group"
    value       = azurerm_resource_group_cost_management_export.resource_group_30day_cost_export.id
}