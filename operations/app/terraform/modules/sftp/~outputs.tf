output "sftp_storage" {
  value = azurerm_storage_account.sftp
}

output "sftp_shares" {
  value = toset(jsondecode(data.external.sftp_ssh_query.result.shares))
}

output "sftp_instances" {
  value = module.instance
}

output "sftp_instance_ids" {
  value = [for k, v in module.instance : v.sftp_id]
}
