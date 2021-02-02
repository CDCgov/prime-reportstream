terraform {
    required_version = ">= 0.14"
}

resource "azurerm_storage_account" "storage_account" {
  resource_group_name = var.resource_group
  name = var.name
  location = var.location
  account_tier = "Standard"
  account_replication_type = "LRS"

  network_rules {
    default_action = "Deny"
    virtual_network_subnet_ids = var.subnet_ids
  }
  
  tags = {
    environment = var.environment
  }
}
