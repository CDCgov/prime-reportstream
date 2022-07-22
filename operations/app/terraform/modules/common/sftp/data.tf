# Public key of pre-generated Azure SSH Key
data "azurerm_ssh_public_key" "sftp" {
  for_each = toset(local.instance_users)

  name                = "pdh${var.environment}-${each.value}"
  resource_group_name = var.resource_group
}
