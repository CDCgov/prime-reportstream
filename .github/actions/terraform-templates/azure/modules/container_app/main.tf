resource "azurerm_container_app_environment" "default" {
  name                = "capp-env-${var.key}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name
}

resource "azurerm_storage_share" "default" {
  name                 = "capp-share-${var.key}"
  quota                = "200"
  storage_account_name = var.storage_account.name
}

resource "azurerm_container_app_environment_storage" "example" {
  name                         = "capp-env-storage-${var.key}"
  container_app_environment_id = azurerm_container_app_environment.default.id
  account_name                 = var.storage_account.name
  share_name                   = azurerm_storage_share.default.name
  access_key                   = var.storage_account.primary_access_key
  access_mode                  = "ReadWrite"
}

resource "azurerm_container_app" "default" {
  name                         = "capp-${var.key}"
  container_app_environment_id = azurerm_container_app_environment.default.id
  resource_group_name          = var.common.resource_group.name
  revision_mode                = "Single"

  ingress {
    allow_insecure_connections = true
    target_port                = 22
    transport                  = "auto"
    traffic_weight {
      latest_revision = true
      percentage      = 100
    }
  }
  template {
    container {
      name   = "examplecontainerapp"
      image  = "mcr.microsoft.com/azure-dev-cli-apps:latest"
      cpu    = 1.0
      memory = "2Gi"
      command = [
        "/bin/bash",
        "-c",
        "echo 'Hello, Azure Container App!' && sleep infinity"
      ]
      volume_mounts {
        name = azurerm_storage_share.default.name
        path = "/mnt/storage"
      }
    }
    volume {
      name         = azurerm_storage_share.default.name
      storage_name = azurerm_container_app_environment_storage.example.name
      storage_type = "AzureFile"
    }
  }
}