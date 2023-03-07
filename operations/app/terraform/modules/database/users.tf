data "http" "icanhazip" {
  url = "http://icanhazip.com"
}

locals {
  public_ip = chomp(data.http.icanhazip.body)
}

locals {
  postgres_readonly_cmd = <<-EOT
      sleep 60;
      az postgres server update -g ${var.resource_group} -n ${azurerm_postgresql_server.postgres_server.name} --public-network-access "Enabled"
      az postgres server firewall-rule create -g ${var.resource_group} -s ${azurerm_postgresql_server.postgres_server.name} \
        -n terraform_runner_${var.resource_prefix} --start-ip-address ${local.public_ip} --end-ip-address ${local.public_ip}

      sleep 60;
      sudo PGPASSWORD=${var.postgres_pass} \
      PGSSLMODE=require \
      psql -h ${azurerm_postgresql_server.postgres_server.fqdn} -U ${var.postgres_user}@${azurerm_postgresql_server.postgres_server.name} \
        -d prime_data_hub \
        -c "CREATE ROLE ${var.postgres_readonly_user} WITH LOGIN PASSWORD '${var.postgres_readonly_pass}'" &> /dev/null

      sleep 5;
      sudo PGPASSWORD=${var.postgres_pass} \
      PGSSLMODE=require \
      psql -h ${azurerm_postgresql_server.postgres_server.fqdn} -U ${var.postgres_user}@${azurerm_postgresql_server.postgres_server.name} \
        -d prime_data_hub \
        -c "ALTER ROLE ${var.postgres_readonly_user} WITH LOGIN PASSWORD '${var.postgres_readonly_pass}'" \
        -c "GRANT SELECT ON ALL TABLES IN SCHEMA public TO ${var.postgres_readonly_user}"

      az postgres server firewall-rule delete -g ${var.resource_group} -s ${azurerm_postgresql_server.postgres_server.name} \
        -n terraform_runner_${var.resource_prefix} --yes
      az postgres server update -g ${var.resource_group} -n ${azurerm_postgresql_server.postgres_server.name} --public-network-access "Disabled"
    EOT
}

resource "null_resource" "postgres_readonly_role" {
  provisioner "local-exec" {
    command = local.postgres_readonly_cmd
  }

  depends_on = [
    azurerm_postgresql_server.postgres_server,
    data.http.icanhazip
  ]
}
