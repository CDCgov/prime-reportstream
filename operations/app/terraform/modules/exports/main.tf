data "azurerm_resource_group" "resource_group" {
    name = var.resource_group_name
}

resource "azurerm_storage_container" "exports_container" {
    name                 = "exports"
    storage_account_name = var.storage_account_name
}

resource "azurerm_resource_group_cost_management_export" "resource_group_30day_cost_export" {
    name                         = "${var.resource_prefix}-rg-30day-cost-export"
    resource_group_id            = data.azurerm_resource_group.resource_group.id
    recurrence_type              = "Daily"
    recurrence_period_start_date = "2024-05-13T00:00:00Z"
    recurrence_period_end_date   = "2034-05-13T00:00:00Z"

    export_data_storage_location {
        container_id     = azurerm_storage_container.exports_container.resource_manager_id
        root_folder_path = "/exports"
    }

    export_data_options {
        type       = "Usage"
        time_frame = "Custom"
    }
}