resource "azurerm_role_assignment" "dev_roles" {
  for_each = toset(var.dev_roles)

  scope                = var.common.resource_group.id
  role_definition_name = each.value
  principal_id         = var.common.owner.object_id
}

resource "azuread_group" "sqladmins" {
  display_name     = "sqladmins-${var.common.env}"
  owners           = [var.common.owner.object_id]
  security_enabled = true

  members = [
    var.common.owner.object_id,
    /* more users */
  ]
}
