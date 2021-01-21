terraform {
    required_version = ">= 0.14"
}

resource "azurerm_storage_account" "storage_account" {
  resource_group_name = var.resource_group
  name = var.name
  location = var.location
  account_tier = "Standard"
  account_replication_type = "LRS"
  tags = {
    environment = var.environment
  }
}
