locals {
  sftp_dir  = "../../../../../.environment/sftp"
  sshnames  = jsondecode(data.external.sftp_ssh_query.result.sshnames)
  instances = toset(jsondecode(data.external.sftp_ssh_query.result.instances))
}
