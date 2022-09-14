# Fetch names of related SSH keys
# SSH key names determine SFTP instances and users
data "external" "sftp_ssh_query" {
  program = ["bash", "${local.sftp_dir}/get_ssh_list.sh"]

  query = {
    environment = "${var.environment}"
  }
}
