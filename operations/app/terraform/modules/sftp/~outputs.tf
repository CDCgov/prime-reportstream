output "sftp_storage" {
  value = azurerm_storage_account.sftp
}

output "sftp_shares" {
  value = toset(jsondecode(data.external.sftp_ssh_query.result.shares))
}
