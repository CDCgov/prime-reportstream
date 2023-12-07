resource "azurerm_container_registry" "container_registry" {
  name                = "${var.resource_prefix}containerregistry"
  resource_group_name = var.resource_group
  location            = var.location
  sku                 = "Premium"

  # Sonarcloud flag
  # Current used for app DOCKER_REGISTRY_SERVER_PASSWORD
  admin_enabled = true
  # Publish docker workflow runs docker login
  public_network_access_enabled = true

  identity {
    type = "SystemAssigned"
  }

  retention_policy {
    days    = 365
    enabled = true
  }

  georeplications {
    location = "westus"
  }

  # network_rule_set {
  #   default_action = "Allow"

  #   virtual_network {
  #     action    = "Allow"
  #     subnet_id = var.public_subnets[0]
  #   }
  #   virtual_network {
  #     action    = "Allow"
  #     subnet_id = var.public_subnets[2]
  #   }
  # }

  trust_policy {
    enabled = "true"
  }

  tags = {
    environment = var.environment
  }
}
