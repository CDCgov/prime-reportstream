output "fqdn" {
  value       = azurerm_container_group.sftp.fqdn
  description = "FQDN of the sftp server"
}

output "ip_address" {
  value       = azurerm_container_group.sftp.ip_address
  description = "FQDN of the sftp server"
}

output "sftp_id" {
  value       = azurerm_container_group.sftp.id
  description = "ID of the sftp server"
}

output "shares" {
  value       = azurerm_storage_share.sftp
  description = "File shares for sftp"
}

output "share_names" {
  value       = values(azurerm_storage_share.sftp)[*].name
  description = "File share names for sftp"
}
