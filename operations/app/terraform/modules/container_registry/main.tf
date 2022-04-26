resource "azurerm_container_registry" "container_registry" {
  name                = "${var.resource_prefix}containerregistry"
  resource_group_name = var.resource_group
  location            = var.location
  sku                 = "Premium"

  # Sonarcloud flag
  admin_enabled                 = false
  public_network_access_enabled = false

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
