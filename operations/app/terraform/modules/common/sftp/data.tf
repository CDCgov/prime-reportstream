# Public key of pre-generated Azure SSH Key
data "azurerm_ssh_public_key" "sftp" {
  for_each = toset(local.file_list_names)

  name                = "pdh${var.environment}-sftp-${each.value}"
  resource_group_name = var.resource_group
}
