# Public key of pre-generated Azure SSH Key
data "azurerm_ssh_public_key" "sftp" {
  for_each = toset(local.file_list_names)

  name                = "pdh${var.environment}-sftp-${each.value}"
  resource_group_name = var.resource_group
}

data "azurerm_storage_blob" "sftp" {
  for_each = toset(["ssh_host_ed25519_key", "ssh_host_ed25519_key.pub", "ssh_host_rsa_key", "ssh_host_rsa_key.pub"])

  name                   = each.value
  storage_account_name   = var.storage_account.name
  storage_container_name = "pdh${var.environment}-sftp"
}
