resource "azurerm_container_registry" "container_registry" {
  name                = "${var.resource_prefix}containerregistry"
  resource_group_name = var.resource_group
  location            = var.location
  sku                 = "Standard"
  admin_enabled       = true

  # network_rule_set {
  #   default_action = "Allow"

  #   virtual_network {
  #     count = var.public_subnets
  #     action    = "Allow"
  #     subnet_id = var.public_subnets[count.index]
  #   }
  # }

  # trust_policy {
  #   enabled = var.enable_content_trust
  # }

  tags = {
    environment = var.environment
  }
}
