module "instance" {
  for_each = var.sshinstances

  source             = "../common/sftp"
  instance           = each.value
  resource_prefix    = "${var.resource_prefix}-${replace(replace(each.value, "/[^a-z0-9]/", ""), "conf", "")}"
  resource_group     = var.resource_group
  environment        = var.environment
  cpu                = 2
  memory             = 4
  sftp_folder        = "uploads"
  location           = var.location
  users              = [for item in var.sshnames : replace(item, "${var.resource_prefix}-${each.value}-", "") if can(regex("${each.value}-", item))]
  instance_users     = [for item in var.sshnames : replace(item, "${var.resource_prefix}-", "") if can(regex("${each.value}-", item))]
  sshaccess          = [for item in var.sshnames : "${replace(item, "${var.resource_prefix}-${each.value}-", "")}:::100" if can(regex("${each.value}-", item))]
  key_vault_id       = var.key_vault_id
  storage_account    = azurerm_storage_account.sftp
  admin_share        = azurerm_storage_share.sftp_admin
  scripts_share      = azurerm_storage_share.sftp_scripts
  nat_gateway_id     = var.nat_gateway_id
  network_profile_id = azurerm_network_profile.sftp.id
  subnet_id          = data.azurerm_subnet.container_subnet.id

  depends_on = [
    azurerm_storage_share_file.sftp,
    azurerm_storage_share.sftp_scripts
  ]
}
