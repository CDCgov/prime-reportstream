resource "azurerm_data_factory" "default" {
  name                = "df-${var.common.uid}-${var.common.env}-${var.key}"
  location            = var.common.location
  resource_group_name = var.common.resource_group.name

  identity {
    type = "SystemAssigned"
  }
}

resource "azurerm_role_assignment" "default" {
  for_each = var.roles

  scope                = var.common.resource_group.id
  role_definition_name = each.value
  principal_id         = azurerm_data_factory.default.identity[0].principal_id
}
