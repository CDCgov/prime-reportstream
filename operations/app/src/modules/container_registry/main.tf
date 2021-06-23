resource "azurerm_container_registry" "container_registry" {
  name = "${var.resource_prefix}containerregistry"
  resource_group_name = var.resource_group
  location = var.location
  sku = "Premium"
  admin_enabled = true

  network_rule_set {
    default_action = "Allow"

    virtual_network {
      action = "Allow"
      subnet_id = data.azurerm_subnet.public.id
    }
  }

  tags = {
    environment = var.environment
  }
}